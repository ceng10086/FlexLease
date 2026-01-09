package com.flexlease.order.dto;

/**
 * DisputeAiSuggestionRequest 请求 DTO。
 */
public record DisputeAiSuggestionRequest(
        String tone,
        Boolean force
) {
}
