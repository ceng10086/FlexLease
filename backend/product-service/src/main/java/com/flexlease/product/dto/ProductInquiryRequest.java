package com.flexlease.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 消费者在下单前提交的商品咨询请求（默认 72 小时有效）。
 */
public record ProductInquiryRequest(
        @Size(max = 100) String contactName,
        @Size(max = 120) String contactMethod,
        @NotBlank(message = "咨询内容不能为空")
        @Size(max = 1000) String message
) {
}
