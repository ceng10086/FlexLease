package com.flexlease.order.domain;

/**
 * 订单状态机枚举。
 * <p>
 * 状态流转由领域对象 {@code RentalOrder} 负责校验；业务接口/调度器按状态决定可执行动作。
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    CANCELLED,
    AWAITING_SHIPMENT,
    AWAITING_RECEIPT,
    IN_LEASE,
    RETURN_REQUESTED,
    RETURN_IN_PROGRESS,
    COMPLETED,
    BUYOUT_REQUESTED,
    BUYOUT_COMPLETED,
    EXCEPTION_CLOSED
}
