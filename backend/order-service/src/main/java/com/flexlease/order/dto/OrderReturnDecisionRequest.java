package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * OrderReturnDecisionRequest 请求 DTO。
 */
public record OrderReturnDecisionRequest(
        @NotNull UUID vendorId,
        boolean approve,
        String remark
) {
}
