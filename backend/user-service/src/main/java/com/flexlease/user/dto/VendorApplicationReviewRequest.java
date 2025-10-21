package com.flexlease.user.dto;

import jakarta.validation.constraints.NotBlank;

public record VendorApplicationReviewRequest(
        @NotBlank(message = "remark 不能为空")
        String remark,

        @NotBlank(message = "reviewerId 不能为空")
        String reviewerId
) {
}
