package com.flexlease.payment.domain;

/**
 * 支付流水状态。
 */
public enum PaymentStatus {
    /** 待支付。 */
    PENDING,
    /** 支付成功。 */
    SUCCEEDED,
    /** 支付失败。 */
    FAILED
}
