package com.flexlease.common.exception;

public enum ErrorCode {
    SUCCESS(0, "ok"),
    VALIDATION_ERROR(1001, "参数校验失败"),
    RESOURCE_NOT_FOUND(1002, "资源不存在"),
    DUPLICATE_RESOURCE(1003, "资源已存在"),
    UNAUTHORIZED(2001, "未认证"),
    FORBIDDEN(2003, "无权访问"),
    INVALID_CREDENTIALS(2004, "用户名或密码错误"),
    INTERNAL_ERROR(5000, "系统异常");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
