package com.flexlease.payment.domain;

/**
 * 支付场景。
 *
 * <p>用于区分同一订单下不同类型的支付：押金/租金/买断/违约金。</p>
 */
public enum PaymentScene {
    /** 押金。 */
    DEPOSIT,
    /** 租金。 */
    RENT,
    /** 买断款。 */
    BUYOUT,
    /** 违约金。 */
    PENALTY
}
