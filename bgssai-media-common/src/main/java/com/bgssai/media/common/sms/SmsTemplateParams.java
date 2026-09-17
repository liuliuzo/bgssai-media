package com.bgssai.media.common.sms;

import com.bgssai.media.common.web.BizException;

/**
 * 腾讯云短信模板参数排列（跨仓统一口径）。
 *
 * <ul>
 *   <li>2 → {@code [code, minutes]}</li>
 *   <li>3 → {@code [product#seq, code, minutes]}</li>
 *   <li>4 → {@code [product, seq, code, minutes]}</li>
 * </ul>
 */
public final class SmsTemplateParams {

    private SmsTemplateParams() {
    }

    public static String[] build(int paramCount, String product, long seq, String code, int expireMinutes) {
        String minutes = String.valueOf(expireMinutes);
        String seqText = String.valueOf(seq);
        switch (paramCount) {
            case 2:
                return new String[] { code, minutes };
            case 3:
                return new String[] { product + "#" + seqText, code, minutes };
            case 4:
                return new String[] { product, seqText, code, minutes };
            default:
                throw new BizException(500, "短信模板参数数量配置无效");
        }
    }
}
