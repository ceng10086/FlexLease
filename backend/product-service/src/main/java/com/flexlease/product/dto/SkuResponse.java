package com.flexlease.product.dto;

import com.flexlease.product.domain.ProductSkuStatus;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * SKU 详情响应（含库存与属性）。
 */
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
