package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaVerifyCode;
import com.bgssai.media.common.mail.PlatformMailSender;
import com.bgssai.media.common.mapper.MediaVerifyCodeMapper;
import com.bgssai.media.common.sms.PlatformSmsSender;
import com.bgssai.media.common.sms.SmsPurpose;
import com.bgssai.media.common.web.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.HexFormat;

/**
 * 登录验证码：生成 → 真实投递 → 落库 → 一次性消费。
 *
 * <p>此前的实现固定发 123456 并把码回给调用方，等于任何人不持有手机 / 邮箱
 * 就能登录任意已有用户。这里的四条约束缺一不可：
 * <ul>
 *   <li><b>真实投递</b>：通道未配置就报错，绝不「假装发送成功」；</li>
 *   <li><b>一次性</b>：校验通过即把 used 置 1，条件更新兼作并发闸门；</li>
 *   <li><b>会过期</b>：expires_at 由通道配置的有效期决定；</li>
 *   <li><b>猜不动</b>：单码尝试次数上限 + 单号码时间窗发码上限。</li>
 * </ul>
 *
 * <p>验证码只以 SHA-256 摘要落库：库被读到时，明文码等同于一批可直接登录的凭证。
 */
@Service
public class VerifyCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerifyCodeService.class);

    /** 同一目标的最小重发间隔（秒）。挡住连点发送把短信费打光。 */
    private static final int RESEND_INTERVAL_SECONDS = 60;

    /** 同一目标 24 小时内最多发码条数。 */
    private static final int DAILY_SEND_LIMIT = 10;

    /** 单个码最多校验次数；与 MediaVerifyCodeMapper.xml 的 attempts &lt; 5 同值。 */
    private static final int MAX_ATTEMPTS = 5;

    /** 邮件通道有效期（秒）；短信通道跟随 platform_sms_config.code_expire_seconds。 */
    private static final int EMAIL_EXPIRE_SECONDS = 600;

    private static final SecureRandom RANDOM = new SecureRandom();

    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_EMAIL = "EMAIL";

    private final MediaVerifyCodeMapper verifyCodeMapper;
    private final PlatformMailSender mailSender;
    private final PlatformSmsSender smsSender;

    public VerifyCodeService(MediaVerifyCodeMapper verifyCodeMapper,
                             PlatformMailSender mailSender,
                             PlatformSmsSender smsSender) {
        this.verifyCodeMapper = verifyCodeMapper;
        this.mailSender = mailSender;
        this.smsSender = smsSender;
    }

    /** 发手机验证码。投递失败不落库，用户可以立即重试。 */
    public void sendPhoneCode(String phone, String scene) {
        String target = normalize(phone);
        if (!target.matches("1[3-9]\\d{9}")) {
            throw new BizException(400, "手机号格式不对");
        }
        guardRate(target);

        String code = generateCode();
        int expireSeconds = smsSender.codeExpireSeconds();
        // 先投递再落库：投递失败就不该留下一条「可用」的码。
        smsSender.sendCode(target, SmsPurpose.of(scene), code);
        persist(target, CHANNEL_SMS, scene, code, expireSeconds);
    }

    /** 发邮箱验证码。 */
    public void sendEmailCode(String email, String scene) {
        String target = normalize(email);
        if (!target.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new BizException(400, "邮箱格式不对");
        }
        guardRate(target);

        String code = generateCode();
        int expireMinutes = EMAIL_EXPIRE_SECONDS / 60;
        mailSender.sendPlainText(target, "BGSSAI Media 登录验证码",
                "您的验证码是 " + code + "，" + expireMinutes + " 分钟内有效。请勿转发给他人。");
        persist(target, CHANNEL_EMAIL, scene, code, EMAIL_EXPIRE_SECONDS);
    }

    /**
     * 校验并消费验证码；失败抛业务异常。
     *
     * <p>校验失败先记一次 attempts 再抛：不计数的话，六位码用几千次请求就能穷举。
     */
    public void consume(String target, String code, String scene) {
        String normalized = normalize(target);
        String usedScene = normalizeScene(scene);
        MediaVerifyCode row = verifyCodeMapper.selectLatestUsable(normalized, usedScene, new Date());
        if (row == null) {
            throw new BizException(401, "验证码不对或已过期");
        }
        if (!sha256(code == null ? "" : code.trim()).equals(row.getCodeHash())) {
            verifyCodeMapper.increaseAttempts(row.getId());
            throw new BizException(401, "验证码不对或已过期");
        }
        // 条件更新兼作并发闸门：两个请求拿同一个码时只有一个能把 used 从 0 改成 1。
        if (verifyCodeMapper.markUsed(row.getId(), new Date()) == 0) {
            throw new BizException(401, "验证码不对或已过期");
        }
    }

    /** 重发间隔 + 单号码日发送上限。两条都是花钱通道的硬闸门。 */
    private void guardRate(String target) {
        Calendar recent = Calendar.getInstance();
        recent.add(Calendar.SECOND, -RESEND_INTERVAL_SECONDS);
        if (verifyCodeMapper.countSentSince(target, recent.getTime()) > 0) {
            throw new BizException(429, "验证码刚发过，请稍后再试");
        }
        Calendar today = Calendar.getInstance();
        today.add(Calendar.HOUR_OF_DAY, -24);
        if (verifyCodeMapper.countSentSince(target, today.getTime()) >= DAILY_SEND_LIMIT) {
            throw new BizException(429, "今日验证码发送次数已达上限");
        }
    }

    private void persist(String target, String channel, String scene, String code, int expireSeconds) {
        Calendar expire = Calendar.getInstance();
        expire.add(Calendar.SECOND, expireSeconds);

        MediaVerifyCode row = new MediaVerifyCode();
        row.setTarget(target);
        row.setChannel(channel);
        row.setScene(normalizeScene(scene));
        row.setCodeHash(sha256(code));
        row.setExpiresAt(expire.getTime());
        verifyCodeMapper.insertSelective(row);
        log.info("验证码已投递 channel={} scene={} expireSeconds={}", channel, normalizeScene(scene), expireSeconds);
    }

    /** SecureRandom 而非 Random：可预测的随机数等同于可预测的验证码。 */
    private static String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1000000));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeScene(String scene) {
        return scene == null || scene.isBlank() ? "LOGIN" : scene.trim().toUpperCase();
    }

    /** 通道就绪状态，供登录页决定是否展示验证码入口。 */
    public boolean smsReady() {
        return smsSender.isConfigured();
    }

    public boolean emailReady() {
        return mailSender.isConfigured();
    }
}
