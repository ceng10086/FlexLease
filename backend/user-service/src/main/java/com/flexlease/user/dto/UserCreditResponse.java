package com.flexlease.user.dto;

import com.flexlease.common.user.CreditTier;
import java.util.UUID;

public record UserCreditResponse(
        UUID userId,
        Integer creditScore,
        CreditTier creditTier
) {
}
