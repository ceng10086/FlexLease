package com.flexlease.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductInquiryRequest(
        @Size(max = 100) String contactName,
        @Size(max = 120) String contactMethod,
        @NotBlank(message = "咨询内容不能为空")
        @Size(max = 1000) String message
) {
}
