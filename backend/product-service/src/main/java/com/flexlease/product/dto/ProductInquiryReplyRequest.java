package com.flexlease.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 厂商回复“下单前咨询”的请求。
 */
public record ProductInquiryReplyRequest(
        @NotBlank(message = "回复内容不能为空")
        @Size(max = 1000)
        String reply
) {
}
