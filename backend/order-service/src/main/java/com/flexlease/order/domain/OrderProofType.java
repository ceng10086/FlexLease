package com.flexlease.order.domain;

/**
 * 取证材料类型枚举。
 * <p>
 * 不同角色可上传的类型不同（由 {@code OrderProofService} 统一校验）。
 */
public enum OrderProofType {
    SHIPMENT,
    RECEIVE,
    RETURN,
    INSPECTION,
    OTHER
}
