package com.flexlease.product.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 媒体资源排序更新请求。
 */
public record MediaSortUpdateRequest(@NotNull Integer sortOrder) {
}
