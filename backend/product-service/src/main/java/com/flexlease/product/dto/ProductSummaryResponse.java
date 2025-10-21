package com.flexlease.product.dto;

import com.flexlease.product.domain.ProductStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        UUID vendorId,
        String name,
        String categoryCode,
        ProductStatus status,
        OffsetDateTime submittedAt,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt
) {
}
