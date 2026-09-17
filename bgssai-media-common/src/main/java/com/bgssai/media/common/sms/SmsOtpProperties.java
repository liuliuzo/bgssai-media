package com.bgssai.media.common.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 短信 OTP 产品名与模板参数个数。
 *
 * <p>跨仓约定（skeleton / wiki / note）：
 * <ul>
 *   <li>{@code bgssai.sms.product-label} — 短信正文里的产品名，本仓默认 Magic</li>
 *   <li>{@code bgssai.sms.template-param-count} — 2|3|4，决定模板参数排列</li>
 * </ul>
 */
@Component
public class SmsOtpProperties {

    private final String productLabel;
    private final int templateParamCount;

    public SmsOtpProperties(
            @Value("${bgssai.sms.product-label:Magic}") String productLabel,
            @Value("${bgssai.sms.template-param-count:3}") int templateParamCount) {
        String trimmed = productLabel == null ? "" : productLabel.trim();
        this.productLabel = trimmed.isEmpty() ? "Magic" : trimmed;
        if (templateParamCount != 2 && templateParamCount != 3 && templateParamCount != 4) {
            throw new IllegalArgumentException(
                    "bgssai.sms.template-param-count must be 2, 3, or 4; got " + templateParamCount);
        }
        this.templateParamCount = templateParamCount;
    }

    public String getProductLabel() {
        return productLabel;
    }

    public int getTemplateParamCount() {
        return templateParamCount;
    }
}
