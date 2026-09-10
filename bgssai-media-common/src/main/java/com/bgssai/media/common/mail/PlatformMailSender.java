package com.bgssai.media.common.mail;

import com.bgssai.media.common.domain.PlatformEmailConfig;
import com.bgssai.media.common.mapper.PlatformEmailConfigMapper;
import com.bgssai.media.common.web.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * 平台发信通道：按 {@code platform_email_config} 单例行实时构造发送器。
 *
 * <p>每次发信都回库读配置：SMTP 参数由运营在管理端维护，改完要立即生效。
 * 缓存配置就会出现「控制台已改、线上还在用旧授权码」的窗口，而这个窗口里每封信都是失败的。
 *
 * <p>通道未配置齐备时<b>抛异常而不是静默跳过</b>：调用方必须知道这封信没发出去，
 * 否则用户会停在「验证码已发送」的提示上等一封永远不会到的信。
 */
@Service
public class PlatformMailSender {

    private static final Logger log = LoggerFactory.getLogger(PlatformMailSender.class);

    private static final long CONFIG_ID = 1L;

    /** SMTP 连接 / 读写超时（毫秒）。发信卡住会连带占住请求线程，必须给硬上限。 */
    private static final String TIMEOUT_MS = "10000";

    private final PlatformEmailConfigMapper platformEmailConfigMapper;

    public PlatformMailSender(PlatformEmailConfigMapper platformEmailConfigMapper) {
        this.platformEmailConfigMapper = platformEmailConfigMapper;
    }

    public PlatformEmailConfig loadConfig() {
        return platformEmailConfigMapper.selectByPrimaryKey(CONFIG_ID);
    }

    /** 通道是否可用：enabled=1 且 host / username / password 均非空。 */
    public boolean isConfigured() {
        return isUsable(loadConfig());
    }

    public void sendPlainText(String to, String subject, String content) {
        PlatformEmailConfig cfg = loadConfig();
        if (!isUsable(cfg)) {
            log.warn("平台邮件通道未启用或未配置齐备，发信中止 to={}", maskEmail(to));
            throw new BizException(503, "邮件服务尚未配置，请联系管理员");
        }
        if (to == null || to.isBlank() || to.contains("\r") || to.contains("\n")) {
            throw new BizException(400, "收件人地址不合法");
        }
        // 主题去 CR/LF 防邮件头注入：带换行的主题能把额外收件人写进信头。
        String safeSubject = subject == null ? "" : subject.replace("\r", " ").replace("\n", " ");
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFrom(cfg));
            message.setTo(to);
            message.setSubject(safeSubject);
            message.setText(content);
            buildSender(cfg).send(message);
        } catch (RuntimeException ex) {
            // 只记异常类型与 host：SMTP 异常文本里常带账号信息。
            log.warn("平台邮件发送失败 to={} host={} exception={}",
                    maskEmail(to), cfg.getSmtpHost(), ex.getClass().getSimpleName());
            throw new BizException(502, "邮件发送失败，请稍后重试");
        }
        log.info("平台邮件发送完成 to={} host={} port={}", maskEmail(to), cfg.getSmtpHost(), cfg.getSmtpPort());
    }

    private static String resolveFrom(PlatformEmailConfig cfg) {
        String address = notBlank(cfg.getFromAddress()) ? cfg.getFromAddress().trim() : cfg.getSmtpUsername();
        String name = cfg.getFromName();
        return notBlank(name) ? name.trim() + " <" + address + ">" : address;
    }

    private static JavaMailSenderImpl buildSender(PlatformEmailConfig cfg) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(cfg.getSmtpHost());
        if (cfg.getSmtpPort() != null) {
            sender.setPort(cfg.getSmtpPort());
        }
        sender.setUsername(cfg.getSmtpUsername());
        sender.setPassword(cfg.getSmtpPassword());
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(isOn(cfg.getAuthEnable())));
        if (isOn(cfg.getSslEnable())) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        if (isOn(cfg.getStarttlsEnable())) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
        props.put("mail.smtp.timeout", TIMEOUT_MS);
        props.put("mail.smtp.writetimeout", TIMEOUT_MS);
        props.put("mail.mime.charset", "UTF-8");
        return sender;
    }

    private static boolean isUsable(PlatformEmailConfig cfg) {
        return cfg != null
                && isOn(cfg.getEnabled())
                && notBlank(cfg.getSmtpHost())
                && notBlank(cfg.getSmtpUsername())
                && notBlank(cfg.getSmtpPassword());
    }

    private static boolean isOn(Byte flag) {
        return flag != null && flag == 1;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /** abc123@example.com -> a***@example.com。日志里出现完整邮箱等同于把用户名单写进日志。 */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "****";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "****";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
