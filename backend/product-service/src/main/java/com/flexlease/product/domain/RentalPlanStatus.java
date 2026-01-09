package com.flexlease.product.domain;

/**
 * 租赁方案状态。
 */
public enum RentalPlanStatus {
    /** 草稿：创建但未启用（通常不对前台展示）。 */
    DRAFT,
    /** 启用：可被选择并参与下单/试算。 */
    ACTIVE,
    /** 停用：仅保留历史数据，不可新下单。 */
    INACTIVE
}
