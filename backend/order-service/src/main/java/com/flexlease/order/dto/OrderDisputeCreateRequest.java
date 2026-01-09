package com.flexlease.order.dto;

import com.flexlease.order.domain.DisputeResolutionOption;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * OrderDisputeCreateRequest 请求 DTO。
 */
public record OrderDisputeCreateRequest(
        @NotNull(message = "缺少操作人")
        UUID actorId,
        @NotNull(message = "请选择期望的解决方案")
        DisputeResolutionOption option,
        @NotBlank(message = "请描述纠纷原因")
        String reason,
        String remark,
        String phoneMemo,
        List<UUID> attachmentProofIds
) {
}
