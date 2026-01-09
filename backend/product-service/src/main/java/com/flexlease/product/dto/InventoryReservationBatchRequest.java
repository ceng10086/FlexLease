package com.flexlease.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * 库存预占/释放批量请求（通常由订单服务调用内部接口）。
 */
public record InventoryReservationBatchRequest(
        @NotNull UUID referenceId,
        @NotEmpty List<@Valid InventoryReservationItemRequest> items
) {
}
