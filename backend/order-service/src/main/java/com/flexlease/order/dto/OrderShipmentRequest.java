package com.flexlease.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderShipmentRequest(
        @NotNull UUID vendorId,
        @NotBlank String carrier,
        @NotBlank String trackingNumber,
        String message
) {
}
