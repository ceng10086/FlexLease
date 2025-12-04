package com.flexlease.order.domain;

public enum OrderDisputeStatus {
    /** 协商中 */
    OPEN,
    /** 等待普通管理员处理 */
    PENDING_ADMIN,
    /** 申诉后等待复核组处理 */
    PENDING_REVIEW_PANEL,
    /** 双方协商达成一致 */
    RESOLVED,
    /** 平台裁决结案 */
    CLOSED
}

