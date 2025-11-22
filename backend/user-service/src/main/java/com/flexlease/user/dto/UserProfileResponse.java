package com.flexlease.user.dto;

import com.flexlease.common.user.CreditTier;
import com.flexlease.user.domain.UserProfileGender;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        UUID userId,
        String fullName,
        UserProfileGender gender,
        String phone,
        String email,
        String address,
        Integer creditScore,
        CreditTier creditTier,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
