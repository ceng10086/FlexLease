package com.flexlease.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * OrderSurveySubmitRequest 请求 DTO。
 */
public record OrderSurveySubmitRequest(
        @NotNull UUID actorId,
        @Min(1) @Max(5) int rating,
        @Size(max = 500) String comment
) {
}
