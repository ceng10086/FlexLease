package com.flexlease.product.domain;

/**
 * 租赁方案类型（对应不同租赁业务模式）。
 */
public enum RentalPlanType {
    /** 普通租赁（按租期计费）。 */
    STANDARD,
    /** 先租后买（支持买断/优惠规则）。 */
    RENT_TO_OWN,
    /** 以租代售（更偏分期/以租代购的简化表达）。 */
    LEASE_TO_SALE
}
