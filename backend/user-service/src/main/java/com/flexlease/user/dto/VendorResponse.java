package com.flexlease.user.dto;

import com.flexlease.user.domain.VendorStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 厂商资料响应（用于管理端/厂商工作台展示）。
 */
public record VendorResponse(
        UUID id,
        UUID ownerUserId,
        String companyName,
        String contactName,
        String contactPhone,
        String contactEmail,
        String province,
        String city,
        String address,
        VendorStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        VendorCommissionProfileResponse commissionProfile
) {
}
