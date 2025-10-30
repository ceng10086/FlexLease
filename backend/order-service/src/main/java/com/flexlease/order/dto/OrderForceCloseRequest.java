package com.flexlease.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderForceCloseRequest(
        @NotNull(message = "管理员编号不能为空")
        UUID adminId,
        String reason
) {
}
