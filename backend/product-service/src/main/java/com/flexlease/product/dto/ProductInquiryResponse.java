package com.flexlease.product.dto;

import com.flexlease.product.domain.ProductInquiryStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

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
