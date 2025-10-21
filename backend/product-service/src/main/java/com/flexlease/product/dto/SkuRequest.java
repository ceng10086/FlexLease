package com.flexlease.product.dto;

import com.flexlease.product.domain.ProductSkuStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record SkuRequest(
        @NotBlank(message = "skuCode 不能为空")
        @Size(max = 64, message = "skuCode 最长 64 字符")
        String skuCode,

        Map<String, Object> attributes,

        @Min(value = 0, message = "stockTotal 不能小于 0")
        int stockTotal,

        @Min(value = 0, message = "stockAvailable 不能小于 0")
        Integer stockAvailable,

        ProductSkuStatus status
) {
}
