package com.flexlease.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 通用 API 响应包装器。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(0, "ok", null);
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
