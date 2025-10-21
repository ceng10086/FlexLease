package com.flexlease.product.dto;

import com.flexlease.product.domain.InventoryChangeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InventoryAdjustRequest(
        @NotNull(message = "changeType 不能为空")
        InventoryChangeType changeType,

        @Min(value = 1, message = "quantity 需大于 0")
        int quantity,

        UUID referenceId
) {
}
