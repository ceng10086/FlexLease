package com.flexlease.common.exception;

/**
 * 业务异常。
 *
 * <p>用于在业务校验/状态机流转等场景下中断流程，并携带统一的 {@link ErrorCode}。
 * 由 {@link GlobalExceptionHandler} 负责把它转换为标准的 {@code ApiResponse}。</p>
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
