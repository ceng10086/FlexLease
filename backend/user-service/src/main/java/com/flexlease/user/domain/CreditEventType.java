package com.flexlease.user.domain;

public enum CreditEventType {
    KYC_VERIFIED,
    ON_TIME_PAYMENT,
    EARLY_RETURN,
    LATE_PAYMENT,
    FRIENDLY_DISPUTE,
    /** 恶意行为（拒收、拒不退还、需赔偿）：扣 30 分并冻结账号 30 天 */
    MALICIOUS_BEHAVIOR
}
