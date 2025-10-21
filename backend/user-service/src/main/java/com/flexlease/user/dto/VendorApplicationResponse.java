package com.flexlease.user.dto;

import com.flexlease.user.domain.VendorApplicationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VendorApplicationResponse(
        UUID id,
        UUID ownerUserId,
        String companyName,
        String unifiedSocialCode,
        String contactName,
        String contactPhone,
        String contactEmail,
        String province,
        String city,
        String address,
        VendorApplicationStatus status,
        OffsetDateTime submittedAt,
        UUID reviewedBy,
        OffsetDateTime reviewedAt,
        String reviewRemark
) {
}
