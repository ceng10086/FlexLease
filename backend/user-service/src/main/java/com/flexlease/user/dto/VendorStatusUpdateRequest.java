package com.flexlease.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 厂商状态更新请求（启用/停用等）。
 */
public record VendorStatusUpdateRequest(
        @NotBlank(message = "状态不能为空")
        String status
) {
}
