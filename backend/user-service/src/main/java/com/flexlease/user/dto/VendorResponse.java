package com.flexlease.user.dto;

import com.flexlease.user.domain.VendorStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

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
