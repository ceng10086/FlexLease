package com.flexlease.product.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 商品媒体资源响应（图片/视频等）。
 */
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
