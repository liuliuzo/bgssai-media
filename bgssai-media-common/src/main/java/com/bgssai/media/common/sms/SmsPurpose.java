package com.bgssai.media.common.sms;

import com.bgssai.media.common.domain.PlatformSmsConfig;

/**
 * 短信场景。模板按场景分列，绝不互相顶替：短信签名与模板在服务商侧是审核制，
 * 拿「登录验证码」模板发「找回密码」内容会被判内容不符而拒发，且拒发在控制台之外看不见。
 */
public enum SmsPurpose {
    LOGIN {
        @Override public String templateIdOf(PlatformSmsConfig cfg) { return cfg.getLoginTemplateId(); }
    },
    BIND_PHONE {
        @Override public String templateIdOf(PlatformSmsConfig cfg) { return cfg.getBindPhoneTemplateId(); }
    },
    RESET_PASSWORD {
        @Override public String templateIdOf(PlatformSmsConfig cfg) { return cfg.getResetPasswordTemplateId(); }
    };

    public abstract String templateIdOf(PlatformSmsConfig cfg);

    public static SmsPurpose of(String scene) {
        if (scene == null || scene.isBlank()) {
            return LOGIN;
        }
        try {
            return valueOf(scene.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return LOGIN;
        }
    }
}
