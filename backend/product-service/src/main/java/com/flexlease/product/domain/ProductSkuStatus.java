package com.flexlease.product.domain;

/**
 * SKU 状态。
 */
public enum ProductSkuStatus {
    /** 启用：可被选择并参与下单/试算。 */
    ACTIVE,
    /** 停用：仅保留历史数据，不可新下单。 */
    INACTIVE
}
