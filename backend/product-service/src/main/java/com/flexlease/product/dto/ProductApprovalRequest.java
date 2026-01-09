package com.flexlease.product.dto;

/**
 * 管理端商品审核请求（remark 为审核备注）。
 */
public record ProductApprovalRequest(
        String remark
) {
}
