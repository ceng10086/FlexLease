package com.flexlease.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
        @NotBlank(message = "刷新令牌不能为空")
        String refreshToken
) {
}
