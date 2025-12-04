package com.flexlease.order.dto;

import com.flexlease.order.domain.DisputeResolutionOption;
import jakarta.validation.constraints.NotNull;

public record OrderDisputeResolveRequest(
        @NotNull(message = "请选择决议方案")
        DisputeResolutionOption decision,
        Integer penalizeUserDelta,
        String remark,
        /**
         * 是否判定为恶意行为（拒收、拒不退还、需赔偿等）。
         * 若为 true，将触发 -30 分惩罚并冻结账号 30 天。
         */
        Boolean maliciousBehavior
) {
}

