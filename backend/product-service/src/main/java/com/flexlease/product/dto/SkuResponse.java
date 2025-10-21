package com.flexlease.product.dto;

import com.flexlease.product.domain.ProductSkuStatus;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record SkuResponse(
        UUID id,
        String skuCode,
        Map<String, Object> attributes,
        int stockTotal,
        int stockAvailable,
        ProductSkuStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
