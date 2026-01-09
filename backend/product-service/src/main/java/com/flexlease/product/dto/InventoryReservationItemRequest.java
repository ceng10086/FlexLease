package com.flexlease.product.dto;

import com.flexlease.product.domain.InventoryChangeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 库存预占/释放条目。
 */
public record InventoryReservationItemRequest(
        @NotNull UUID skuId,
        @Min(1) int quantity,
        @NotNull InventoryChangeType changeType
) {
}
