package com.flexlease.user.domain;

/**
 * 厂商状态。
 */
public enum VendorStatus {
    /** 正常：可创建商品与处理订单。 */
    ACTIVE,
    /** 暂停：平台停用该厂商（一般会限制关键操作）。 */
    SUSPENDED
}
