package com.flexlease.order.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.user.CreditTier;
import com.flexlease.common.user.CreditTierRules;
import com.flexlease.order.client.UserProfileClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CreditAssessmentService {

    private static final BigDecimal EXCELLENT_RATE = new BigDecimal("0.70");
    private static final BigDecimal WARNING_RATE = new BigDecimal("1.20");
    private static final BigDecimal STANDARD_RATE = BigDecimal.ONE;

    private final UserProfileClient userProfileClient;

    public CreditAssessmentService(UserProfileClient userProfileClient) {
        this.userProfileClient = userProfileClient;
    }

    public CreditDecision assess(UUID userId) {
        UserProfileClient.UserCreditView creditView = userProfileClient.loadCredit(userId);
        int score = CreditTierRules.clampScore(creditView.creditScore());
        CreditTier tier = creditView.creditTier() != null ? creditView.creditTier() : CreditTierRules.tierForScore(score);
        if (tier == CreditTier.RESTRICTED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "信用等级受限，暂无法创建订单");
        }
        BigDecimal adjustmentRate = switch (tier) {
            case EXCELLENT -> EXCELLENT_RATE;
            case WARNING -> WARNING_RATE;
            case STANDARD -> STANDARD_RATE;
            case RESTRICTED -> throw new BusinessException(ErrorCode.FORBIDDEN, "信用等级受限，暂无法创建订单");
        };
        boolean manualReview = tier == CreditTier.WARNING;
        return new CreditDecision(score, tier, adjustmentRate, manualReview);
    }

    public record CreditDecision(int creditScore,
                                 CreditTier creditTier,
                                 BigDecimal depositAdjustmentRate,
                                 boolean requiresManualReview) {
        public BigDecimal apply(BigDecimal baseAmount) {
            if (baseAmount == null) {
                return BigDecimal.ZERO;
            }
            return baseAmount.multiply(depositAdjustmentRate).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
