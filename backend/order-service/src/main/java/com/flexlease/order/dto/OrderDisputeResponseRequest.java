package com.flexlease.order.dto;

import com.flexlease.order.domain.DisputeResolutionOption;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderDisputeResponseRequest(
        @NotNull(message = "缺少操作人")
        UUID actorId,
        @NotNull(message = "请选择方案")
        DisputeResolutionOption option,
        boolean accept,
        String remark
) {
}
