package com.flexlease.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理员更新账号状态请求（冻结/解冻等，实际状态写入认证中心）。
 */
public record UserStatusUpdateRequest(
        @NotBlank(message = "状态不能为空")
        String status
) {
}
