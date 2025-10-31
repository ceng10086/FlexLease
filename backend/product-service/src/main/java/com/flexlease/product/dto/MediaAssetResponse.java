package com.flexlease.product.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MediaAssetResponse(
        UUID id,
        String fileName,
        String fileUrl,
        String contentType,
        Long fileSize,
        Integer sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
