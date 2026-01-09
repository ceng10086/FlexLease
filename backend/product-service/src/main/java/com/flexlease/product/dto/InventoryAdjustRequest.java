package com.flexlease.product.dto;

import com.flexlease.product.domain.InventoryChangeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 库存调整请求（入库/出库/预占/释放）。
 */
public record InventoryAdjustRequest(
        @NotNull(message = "changeType 不能为空")
        InventoryChangeType changeType,

        @Min(value = 1, message = "quantity 需大于 0")
        int quantity,

        UUID referenceId
) {
}
