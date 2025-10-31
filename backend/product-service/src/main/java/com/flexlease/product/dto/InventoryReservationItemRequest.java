package com.flexlease.product.dto;

import com.flexlease.product.domain.InventoryChangeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InventoryReservationItemRequest(
        @NotNull UUID skuId,
        @Min(1) int quantity,
        @NotNull InventoryChangeType changeType
) {
}
