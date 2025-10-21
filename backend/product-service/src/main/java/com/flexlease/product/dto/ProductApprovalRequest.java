package com.flexlease.product.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductApprovalRequest(
        @NotBlank(message = "reviewerId 不能为空")
        String reviewerId,

        String remark
) {
}
