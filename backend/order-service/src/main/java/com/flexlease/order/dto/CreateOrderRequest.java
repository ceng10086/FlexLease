package com.flexlease.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID userId,
        @NotNull UUID vendorId,
        String planType,
        OffsetDateTime leaseStartAt,
        OffsetDateTime leaseEndAt,
        @Valid List<OrderItemRequest> items,
        List<UUID> cartItemIds
) {
}
