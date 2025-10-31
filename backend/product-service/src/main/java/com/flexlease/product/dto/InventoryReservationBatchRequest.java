package com.flexlease.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record InventoryReservationBatchRequest(
        @NotNull UUID referenceId,
        @NotEmpty List<@Valid InventoryReservationItemRequest> items
) {
}
