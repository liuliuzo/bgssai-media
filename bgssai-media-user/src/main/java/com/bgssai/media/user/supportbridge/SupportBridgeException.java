package com.bgssai.media.user.supportbridge;

/** 统一客服桥的业务失败。code 是 HTTP 状态。 */
public class SupportBridgeException extends RuntimeException {
    private final int code;

    public SupportBridgeException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() { return code; }
}
