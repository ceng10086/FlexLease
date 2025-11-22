package com.flexlease.common.user;

/**
 * 统一的信用等级与分值转换规则，保证多服务间口径一致。
 */
public final class CreditTierRules {

    private static final int DEFAULT_SCORE = 60;
    private static final int MAX_SCORE = 100;
    private static final int MIN_SCORE = 0;

    private CreditTierRules() {
    }

    public static int defaultScore() {
        return DEFAULT_SCORE;
    }

    public static int clampScore(Integer score) {
        int value = score == null ? DEFAULT_SCORE : score;
        if (value < MIN_SCORE) {
            return MIN_SCORE;
        }
        if (value > MAX_SCORE) {
            return MAX_SCORE;
        }
        return value;
    }

    public static CreditTier tierForScore(Integer score) {
        int value = clampScore(score);
        if (value >= 90) {
            return CreditTier.EXCELLENT;
        }
        if (value >= 60) {
            return CreditTier.STANDARD;
        }
        if (value >= 40) {
            return CreditTier.WARNING;
        }
        return CreditTier.RESTRICTED;
    }
}
