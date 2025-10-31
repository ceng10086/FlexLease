package com.flexlease.product.dto;

import jakarta.validation.constraints.NotNull;

public record MediaSortUpdateRequest(@NotNull Integer sortOrder) {
}
