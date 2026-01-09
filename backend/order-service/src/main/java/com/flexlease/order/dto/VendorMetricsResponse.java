package com.flexlease.order.dto;

import com.flexlease.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * VendorMetricsResponse 响应 DTO。
 */
public record VendorMetricsResponse(
        UUID vendorId,
        long totalOrders,
        long activeOrders,
        BigDecimal totalGmv,
        long inLeaseCount,
        long pendingReturns,
        Map<OrderStatus, Long> ordersByStatus,
        List<TrendPoint> recentTrend,
        List<PlanTypeMetric> planBreakdown,
        CreditMetrics creditMetrics,
        DisputeMetrics disputeMetrics,
        SurveyMetrics surveyMetrics
) {
}
