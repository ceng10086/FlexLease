package com.flexlease.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 内部信用事件上报请求（订单/认证等服务用于触发积分变更）。
 */
public record CreditEventRequest(
        @NotBlank(message = "事件类型不能为空")
        String eventType,
        Map<String, Object> attributes
) {
}
