package com.flexlease.product.domain;

/**
 * 下单前咨询状态。
 */
public enum ProductInquiryStatus {
    /** 已提交，等待厂商回复。 */
    OPEN,
    /** 已回复。 */
    RESPONDED,
    /** 已过期（超过有效期后自动进入该状态）。 */
    EXPIRED
}
