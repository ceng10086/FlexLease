package com.flexlease.user.dto;

import com.flexlease.common.user.CreditTier;
import com.flexlease.user.domain.UserProfileGender;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 用户档案响应（含信用分、实名认证、冻结信息等）。
 */
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
        boolean kycVerified,
        OffsetDateTime kycVerifiedAt,
        int paymentStreak,
        OffsetDateTime suspendedUntil,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
