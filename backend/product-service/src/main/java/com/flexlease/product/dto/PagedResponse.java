package com.flexlease.product.dto;

import java.util.List;

/**
 * 通用分页响应包装。
 *
 * <p>约定：{@code page} 从 1 开始。</p>
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
