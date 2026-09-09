package com.bgssai.media.common.web;

public class ApiResponse<T> {
    private int code;
    private String message;
    private boolean success;
    private T result;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, boolean success, T result) {
        this.code = code;
        this.message = message;
        this.success = success;
        this.result = result;
    }

    public static <T> ApiResponse<T> ok(T result) {
        return new ApiResponse<>(0, "ok", true, result);
    }

    public static <T> ApiResponse<T> okMessage(String message, T result) {
        return new ApiResponse<>(0, message, true, result);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, false, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
