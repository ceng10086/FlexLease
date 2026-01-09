package com.flexlease.user.dto;

/**
 * 管理员审核厂商入驻申请请求（remark 为审核备注）。
 */
public record VendorApplicationReviewRequest(
        String remark
) {
}
