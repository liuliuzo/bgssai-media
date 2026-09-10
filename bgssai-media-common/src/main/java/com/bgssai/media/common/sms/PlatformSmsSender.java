package com.bgssai.media.common.sms;

import com.bgssai.media.common.domain.PlatformSmsConfig;
import com.bgssai.media.common.mapper.PlatformSmsConfigMapper;
import com.bgssai.media.common.web.BizException;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 平台短信通道：按 {@code platform_sms_config} 单例行实时构造腾讯云客户端。
 *
 * <p>与 {@link com.bgssai.media.common.mail.PlatformMailSender} 同款口径：
 * 配置回库实时读、凭据绝不入日志、通道不可用直接抛异常而不是静默跳过。
 */
@Service
public class PlatformSmsSender {

    private static final Logger log = LoggerFactory.getLogger(PlatformSmsSender.class);

    private static final long CONFIG_ID = 1L;

    private static final String ENDPOINT = "sms.tencentcloudapi.com";

    private static final int TIMEOUT_SECONDS = 10;

    /** 大陆号码的 E.164 前缀。其它区号在入参校验阶段就被拒。 */
    private static final String CN_PREFIX = "+86";

    private final PlatformSmsConfigMapper platformSmsConfigMapper;

    public PlatformSmsSender(PlatformSmsConfigMapper platformSmsConfigMapper) {
        this.platformSmsConfigMapper = platformSmsConfigMapper;
    }

    public PlatformSmsConfig loadConfig() {
        return platformSmsConfigMapper.selectByPrimaryKey(CONFIG_ID);
    }

    /** 通道是否可用：enabled=1 且 secretId / secretKey / sdkAppId / signName 齐备。 */
    public boolean isConfigured() {
        return isUsable(loadConfig());
    }

    /** 验证码有效期（秒）：短信正文里的「X 分钟内有效」由同一个值渲染。 */
    public int codeExpireSeconds() {
        PlatformSmsConfig cfg = loadConfig();
        return cfg == null || cfg.getCodeExpireSeconds() == null ? 300 : cfg.getCodeExpireSeconds();
    }

    /**
     * 发送验证码短信。
     *
     * @param phone   大陆手机号（11 位，不带区号）
     * @param purpose 发码场景，决定用哪个模板
     * @param code    验证码明文（进模板参数，不进日志）
     * @return 服务商 RequestId，用于排障；失败一律抛 {@link BizException}
     */
    public String sendCode(String phone, SmsPurpose purpose, String code) {
        PlatformSmsConfig cfg = loadConfig();
        if (!isUsable(cfg)) {
            log.warn("平台短信通道未启用或未配置齐备，发码中止 phone={}", maskPhone(phone));
            throw new BizException(503, "短信服务尚未配置，请联系管理员");
        }
        String templateId = purpose.templateIdOf(cfg);
        if (templateId == null || templateId.isBlank()) {
            log.warn("短信场景未配置模板 purpose={} phone={}", purpose.name(), maskPhone(phone));
            throw new BizException(503, "短信服务尚未配置该场景模板，请联系管理员");
        }

        int expireMinutes = Math.max(1, codeExpireSeconds() / 60);
        try {
            Credential cred = new Credential(cfg.getSecretId(), cfg.getSecretKey());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(ENDPOINT);
            httpProfile.setConnTimeout(TIMEOUT_SECONDS);
            httpProfile.setReadTimeout(TIMEOUT_SECONDS);
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            SmsClient client = new SmsClient(cred, cfg.getRegion(), clientProfile);
            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(cfg.getSdkAppId());
            req.setSignName(cfg.getSignName());
            req.setTemplateId(templateId);
            req.setPhoneNumberSet(new String[] { CN_PREFIX + phone });
            // 模板参数顺序由服务商侧模板决定，本仓约定统一为 {1}=验证码 {2}=有效分钟数。
            req.setTemplateParamSet(new String[] { code, String.valueOf(expireMinutes) });

            SendSmsResponse resp = client.SendSms(req);
            assertAccepted(resp, phone);
            log.info("短信验证码已发送 purpose={} phone={} requestId={}",
                    purpose.name(), maskPhone(phone), resp.getRequestId());
            return resp.getRequestId();
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("短信发送失败 purpose={} phone={} exception={}",
                    purpose.name(), maskPhone(phone), ex.getClass().getSimpleName());
            throw new BizException(502, "短信发送失败，请稍后重试");
        }
    }

    /**
     * 逐条校验发送状态。
     *
     * <p>腾讯云的 SendSms 即使整体 HTTP 成功，单条号码仍可能因欠费 / 号码格式 / 频控被拒，
     * 状态藏在 SendStatusSet 里。不看这一层就会把「服务商已拒发」当成发送成功。
     */
    private static void assertAccepted(SendSmsResponse resp, String phone) {
        SendStatus[] statuses = resp.getSendStatusSet();
        if (statuses == null || statuses.length == 0) {
            throw new BizException(502, "短信发送失败，请稍后重试");
        }
        for (SendStatus status : statuses) {
            if (!"Ok".equalsIgnoreCase(status.getCode())) {
                log.warn("短信被服务商拒发 phone={} code={} message={}",
                        maskPhone(phone), status.getCode(), status.getMessage());
                throw new BizException(502, "短信发送失败，请稍后重试");
            }
        }
    }

    private static boolean isUsable(PlatformSmsConfig cfg) {
        return cfg != null
                && cfg.getEnabled() != null && cfg.getEnabled() == 1
                && notBlank(cfg.getSecretId())
                && notBlank(cfg.getSecretKey())
                && notBlank(cfg.getSdkAppId())
                && notBlank(cfg.getSignName());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /** 13812345678 -> 138****5678。日志里一律用掩码形态。 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
