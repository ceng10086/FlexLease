package com.flexlease.payment.domain;

/**
 * 退款状态。
 *
 * <p>模拟通道下通常会直接从 {@link #PROCESSING} 进入 {@link #SUCCEEDED}。</p>
 */
public enum RefundStatus {
    /** 处理中。 */
    PROCESSING,
    /** 退款成功。 */
    SUCCEEDED,
    /** 退款失败。 */
    FAILED
}
