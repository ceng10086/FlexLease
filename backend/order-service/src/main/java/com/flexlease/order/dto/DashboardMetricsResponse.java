package com.flexlease.order.dto;

import com.flexlease.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardMetricsResponse(
        long totalOrders,
        long activeOrders,
        BigDecimal totalGmv,
        long inLeaseCount,
        long pendingReturns,
        Map<OrderStatus, Long> ordersByStatus,
        List<TrendPoint> recentTrend,
        List<PlanTypeMetric> planBreakdown
) {
}
