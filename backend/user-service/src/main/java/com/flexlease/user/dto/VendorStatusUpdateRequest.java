package com.flexlease.user.dto;

import jakarta.validation.constraints.NotBlank;

public record VendorStatusUpdateRequest(
        @NotBlank(message = "状态不能为空")
        String status
) {
}
