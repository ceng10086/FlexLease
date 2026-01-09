package com.flexlease.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 商品基础信息创建/更新请求（不包含租赁方案与库存）。
 */
public record ProductRequest(
        @NotBlank(message = "name 不能为空")
        @Size(max = 200, message = "name 最长 200 字符")
        String name,

        @NotBlank(message = "categoryCode 不能为空")
        @Size(max = 100, message = "categoryCode 最长 100 字符")
        String categoryCode,

        @Size(max = 2000, message = "description 最长 2000 字符")
        String description,

        @Size(max = 255, message = "coverImageUrl 最长 255 字符")
        String coverImageUrl
) {
}
