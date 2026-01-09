package com.flexlease.product.domain;

/**
 * 商品状态。
 */
public enum ProductStatus {
    /** 草稿：厂商创建但未提交审核。 */
    DRAFT,
    /** 待审核：提交后等待管理员处理。 */
    PENDING_REVIEW,
    /** 上架：可在前台目录中展示与下单。 */
    ACTIVE,
    /** 下架：不可在前台展示。 */
    INACTIVE,
    /** 已驳回：需要厂商修改后重新提交。 */
    REJECTED
}
