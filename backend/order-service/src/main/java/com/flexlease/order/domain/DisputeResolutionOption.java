package com.flexlease.order.domain;

/**
 * 纠纷预设裁决/协商方案枚举。
 * <p>
 * 前端会以该枚举作为选项基础；若无法匹配预设方案则使用 {@link #CUSTOM}。
 */
public enum DisputeResolutionOption {
    REDELIVER,
    PARTIAL_REFUND,
    RETURN_WITH_DEPOSIT_DEDUCTION,
    DISCOUNTED_BUYOUT,
    CUSTOM
}
