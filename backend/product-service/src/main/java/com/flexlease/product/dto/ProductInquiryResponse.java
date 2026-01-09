package com.flexlease.product.dto;

import com.flexlease.product.domain.ProductInquiryStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 商品咨询响应（消费者提交、厂商回复、过期状态等）。
 */
public record ProductInquiryResponse(
        UUID id,
        UUID productId,
        UUID vendorId,
        UUID requesterId,
        String contactName,
        String contactMethod,
        String message,
        ProductInquiryStatus status,
        String reply,
        OffsetDateTime expiresAt,
        OffsetDateTime respondedAt,
        OffsetDateTime createdAt
) {
}
