package com.flexlease.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserStatusRequest(
        @NotBlank(message = "status 不能为空")
        String status
) {
}
