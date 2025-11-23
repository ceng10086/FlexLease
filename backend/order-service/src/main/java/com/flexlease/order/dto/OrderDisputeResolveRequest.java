package com.flexlease.order.dto;

import com.flexlease.order.domain.DisputeResolutionOption;
import jakarta.validation.constraints.NotNull;

public record OrderDisputeResolveRequest(
        @NotNull(message = "请选择决议方案")
        DisputeResolutionOption decision,
        Integer penalizeUserDelta,
        String remark
) {
}
