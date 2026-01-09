package com.flexlease.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * UpdateCartItemRequest 请求 DTO。
 */
public record UpdateCartItemRequest(
        @NotNull UUID userId,
        @Min(1) int quantity
) {
}
