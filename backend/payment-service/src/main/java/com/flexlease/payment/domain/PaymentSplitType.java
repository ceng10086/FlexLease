package com.flexlease.payment.domain;

/**
 * 分账类型。
 *
 * <p>用于表达一笔支付金额在不同受益人之间的拆分结果。</p>
 */
public enum PaymentSplitType {
    /** 平台抽成（平台收益）。 */
    PLATFORM_COMMISSION,
    /** 厂商实收（扣除抽成后的净额）。 */
    VENDOR_INCOME,
    /** 押金留存（押金不进入厂商收益，待退租时原路退回）。 */
    DEPOSIT_RESERVE
}
