package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.order.dto.DashboardMetricsResponse;
import com.flexlease.order.dto.VendorMetricsResponse;
import com.flexlease.order.service.OrderAnalyticsService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final OrderAnalyticsService orderAnalyticsService;

    public AnalyticsController(OrderAnalyticsService orderAnalyticsService) {
        this.orderAnalyticsService = orderAnalyticsService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardMetricsResponse> dashboard() {
        return ApiResponse.success(orderAnalyticsService.getDashboardMetrics());
    }

    @GetMapping("/vendor/{vendorId}")
    public ApiResponse<VendorMetricsResponse> vendorMetrics(@PathVariable String vendorId) {
        try {
            UUID vendorUuid = UUID.fromString(vendorId);
            return ApiResponse.success(orderAnalyticsService.getVendorMetrics(vendorUuid));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "vendorId 需为合法 UUID");
        }
    }
}
