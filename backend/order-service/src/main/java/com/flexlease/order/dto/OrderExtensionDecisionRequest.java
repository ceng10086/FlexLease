package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * OrderExtensionDecisionRequest 请求 DTO。
 */
public record OrderExtensionDecisionRequest(
        @NotNull UUID vendorId,
        boolean approve,
        String remark
) {
}
