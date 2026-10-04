package com.bgssai.media.user.supportbridge;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** 与 bgssai-support 一致：对原始 JSON 字节做 HMAC-SHA256，头为 sha256=&lt;hex&gt;。 */
public final class SupportSignatures {

    private SupportSignatures() {
    }

    public static String sign(String secret, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(body));
        } catch (Exception ex) {
            throw new IllegalStateException("无法计算客服签名", ex);
        }
    }

    public static boolean matches(String secret, byte[] body, String header) {
        if (secret == null || secret.isBlank() || body == null || header == null || header.isBlank()) {
            return false;
        }
        String supplied = header.trim();
        if (supplied.regionMatches(true, 0, "sha256=", 0, 7)) {
            supplied = supplied.substring(7);
        }
        if (!supplied.matches("(?i)[a-f0-9]{64}")) {
            return false;
        }
        byte[] expected = decodeHex(sign(secret, body));
        byte[] actual = decodeHex(supplied.toLowerCase(Locale.ROOT));
        return expected != null && actual != null && MessageDigest.isEqual(expected, actual);
    }

    private static String toHex(byte[] raw) {
        StringBuilder out = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            out.append(Character.forDigit((b >> 4) & 0xF, 16));
            out.append(Character.forDigit(b & 0xF, 16));
        }
        return out.toString();
    }

    private static byte[] decodeHex(String hex) {
        if (hex.length() % 2 != 0) {
            return null;
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                return null;
            }
            out[i] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
