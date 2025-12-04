package com.flexlease.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record CreditEventRequest(
        @NotBlank(message = "事件类型不能为空")
        String eventType,
        Map<String, Object> attributes
) {
}
