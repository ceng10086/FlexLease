package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * OrderCancelRequest 请求 DTO。
 */
public record OrderCancelRequest(
        @NotNull UUID userId,
        String reason
) {
}
