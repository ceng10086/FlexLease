package com.flexlease.product.domain;

/**
 * 库存变更类型。
 */
public enum InventoryChangeType {
    /** 入库：库存增加（退租归还、补货等）。 */
    INBOUND,
    /** 出库：库存减少（发货/履约消耗等）。 */
    OUTBOUND,
    /** 预占：下单前冻结可用库存。 */
    RESERVE,
    /** 释放：取消/失败后释放预占库存。 */
    RELEASE
}
