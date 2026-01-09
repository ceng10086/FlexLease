package com.flexlease.order.dto;

import java.util.List;

/**
 * PagedResponse 响应 DTO。
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
