package com.flexlease.order.service;

import com.flexlease.common.user.CreditTier;
import com.flexlease.order.domain.OrderDisputeStatus;
import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.domain.OrderSurveyStatus;
import com.flexlease.order.dto.DashboardMetricsResponse;
import com.flexlease.order.dto.DisputeMetrics;
import com.flexlease.order.dto.PlanTypeMetric;
import com.flexlease.order.dto.TrendPoint;
import com.flexlease.order.dto.VendorMetricsResponse;
import com.flexlease.order.dto.CreditMetrics;
import com.flexlease.order.dto.SurveyMetrics;
import com.flexlease.order.repository.OrderDisputeRepository;
import com.flexlease.order.repository.OrderDisputeRepository.ResolutionMetric;
import com.flexlease.order.repository.OrderSatisfactionSurveyRepository;
import com.flexlease.order.repository.RentalOrderRepository;
import com.flexlease.order.repository.RentalOrderRepository.OrderStatusCount;
import com.flexlease.order.repository.RentalOrderRepository.CreditTierAggregate;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@Transactional(Transactional.TxType.SUPPORTS)
public class OrderAnalyticsService {

    private static final int TREND_WINDOW_DAYS = 7;

    private static final List<OrderStatus> ACTIVE_STATUSES = List.of(
            OrderStatus.AWAITING_SHIPMENT,
            OrderStatus.AWAITING_RECEIPT,
            OrderStatus.IN_LEASE,
            OrderStatus.RETURN_REQUESTED,
            OrderStatus.RETURN_IN_PROGRESS,
            OrderStatus.BUYOUT_REQUESTED
    );

    private static final List<OrderStatus> GMV_STATUSES = List.of(
            OrderStatus.AWAITING_SHIPMENT,
            OrderStatus.AWAITING_RECEIPT,
            OrderStatus.IN_LEASE,
            OrderStatus.RETURN_REQUESTED,
            OrderStatus.RETURN_IN_PROGRESS,
            OrderStatus.COMPLETED,
            OrderStatus.BUYOUT_REQUESTED,
            OrderStatus.BUYOUT_COMPLETED
    );

    private final RentalOrderRepository rentalOrderRepository;
    private final OrderDisputeRepository orderDisputeRepository;
    private final OrderSatisfactionSurveyRepository surveyRepository;

    public OrderAnalyticsService(RentalOrderRepository rentalOrderRepository,
                                 OrderDisputeRepository orderDisputeRepository,
                                 OrderSatisfactionSurveyRepository surveyRepository) {
        this.rentalOrderRepository = rentalOrderRepository;
        this.orderDisputeRepository = orderDisputeRepository;
        this.surveyRepository = surveyRepository;
    }

    public DashboardMetricsResponse getDashboardMetrics() {
        long totalOrders = rentalOrderRepository.count();
        long activeOrders = rentalOrderRepository.countByStatusIn(ACTIVE_STATUSES);
        long inLease = rentalOrderRepository.countByStatus(OrderStatus.IN_LEASE);
        long pendingReturns = rentalOrderRepository.countByStatus(OrderStatus.RETURN_REQUESTED);
        BigDecimal totalGmv = safeSum(rentalOrderRepository.sumTotalAmountByStatusIn(GMV_STATUSES));
        Map<OrderStatus, Long> statusMap = toStatusMap(rentalOrderRepository.aggregateStatus());
        TrendWindow window = buildTrendWindow();
        List<TrendPoint> trend = buildTrend(
                rentalOrderRepository.aggregateDailyMetrics(window.start(), window.end()),
                window
        );
        List<PlanTypeMetric> planMetrics = toPlanMetrics(rentalOrderRepository.aggregatePlanTypeMetrics());
        CreditMetrics creditMetrics = buildCreditMetrics(
                rentalOrderRepository.averageCreditScore(),
                rentalOrderRepository.aggregateCreditTierMetrics()
        );
        DisputeMetrics disputeMetrics = buildDisputeMetrics(null);
        SurveyMetrics surveyMetrics = buildSurveyMetrics(null);
        return new DashboardMetricsResponse(
                totalOrders,
                activeOrders,
                totalGmv,
                inLease,
                pendingReturns,
                statusMap,
                trend,
                planMetrics,
                creditMetrics,
                disputeMetrics,
                surveyMetrics
        );
    }

    public VendorMetricsResponse getVendorMetrics(UUID vendorId) {
        long totalOrders = rentalOrderRepository.countByVendorId(vendorId);
        long activeOrders = rentalOrderRepository.countByVendorIdAndStatusIn(vendorId, ACTIVE_STATUSES);
        long inLease = rentalOrderRepository.countByVendorIdAndStatus(vendorId, OrderStatus.IN_LEASE);
        long pendingReturns = rentalOrderRepository.countByVendorIdAndStatus(vendorId, OrderStatus.RETURN_REQUESTED);
        BigDecimal totalGmv = safeSum(rentalOrderRepository.sumTotalAmountByVendorIdAndStatusIn(vendorId, GMV_STATUSES));
        Map<OrderStatus, Long> statusMap = toStatusMap(rentalOrderRepository.aggregateStatusByVendor(vendorId));
        TrendWindow window = buildTrendWindow();
        List<TrendPoint> trend = buildTrend(
                rentalOrderRepository.aggregateDailyMetricsByVendor(vendorId, window.start(), window.end()),
                window
        );
        List<PlanTypeMetric> planMetrics = toPlanMetrics(rentalOrderRepository.aggregatePlanTypeMetricsByVendor(vendorId));
        CreditMetrics creditMetrics = buildCreditMetrics(
                rentalOrderRepository.averageCreditScoreByVendor(vendorId),
                rentalOrderRepository.aggregateCreditTierMetricsByVendor(vendorId)
        );
        DisputeMetrics disputeMetrics = buildDisputeMetrics(vendorId);
        SurveyMetrics surveyMetrics = buildSurveyMetrics(vendorId);
        return new VendorMetricsResponse(
                vendorId,
                totalOrders,
                activeOrders,
                totalGmv,
                inLease,
                pendingReturns,
                statusMap,
                trend,
                planMetrics,
                creditMetrics,
                disputeMetrics,
                surveyMetrics
        );
    }

    private Map<OrderStatus, Long> toStatusMap(List<OrderStatusCount> aggregates) {
        EnumMap<OrderStatus, Long> map = new EnumMap<>(OrderStatus.class);
        for (OrderStatusCount aggregate : aggregates) {
            map.put(aggregate.getStatus(), aggregate.getCount());
        }
        return map;
    }

    private BigDecimal safeSum(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private TrendWindow buildTrendWindow() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime end = now.withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(999_000_000);
        OffsetDateTime start = end.minusDays(TREND_WINDOW_DAYS - 1L)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        return new TrendWindow(start, end, start.toLocalDate(), end.toLocalDate());
    }

    private List<TrendPoint> buildTrend(List<RentalOrderRepository.DailyMetric> aggregates, TrendWindow window) {
        Map<LocalDate, RentalOrderRepository.DailyMetric> metricMap = new HashMap<>();
        for (RentalOrderRepository.DailyMetric aggregate : aggregates) {
            metricMap.put(aggregate.getDay(), aggregate);
        }
        List<TrendPoint> result = new ArrayList<>();
        LocalDate cursor = window.startDate();
        while (!cursor.isAfter(window.endDate())) {
            RentalOrderRepository.DailyMetric metric = metricMap.get(cursor);
            long orders = metric == null ? 0 : metric.getOrderCount();
            BigDecimal gmv = metric == null ? BigDecimal.ZERO : safeSum(metric.getTotalAmount());
            result.add(new TrendPoint(cursor, orders, gmv));
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private List<PlanTypeMetric> toPlanMetrics(List<RentalOrderRepository.PlanTypeAggregate> aggregates) {
        return aggregates.stream()
                .map(aggregate -> new PlanTypeMetric(
                        normalizePlanType(aggregate.getPlanType()),
                        aggregate.getOrderCount(),
                        safeSum(aggregate.getTotalAmount())
                ))
                .sorted(Comparator.comparingLong(PlanTypeMetric::orders).reversed())
                .toList();
    }

    private String normalizePlanType(String rawPlanType) {
        return (rawPlanType == null || rawPlanType.isBlank()) ? "UNKNOWN" : rawPlanType;
    }

    private CreditMetrics buildCreditMetrics(Double averageScoreRaw, List<CreditTierAggregate> aggregates) {
        BigDecimal averageScore = averageScoreRaw == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(averageScoreRaw).setScale(2, RoundingMode.HALF_UP);
        EnumMap<CreditTier, Long> map = new EnumMap<>(CreditTier.class);
        for (CreditTierAggregate aggregate : aggregates) {
            map.put(aggregate.getCreditTier(), aggregate.getCount());
        }
        return new CreditMetrics(averageScore, map);
    }

    private DisputeMetrics buildDisputeMetrics(UUID vendorId) {
        long open;
        long pendingAdmin;
        long resolvedCount;
        List<ResolutionMetric> metrics;
        List<OrderDisputeStatus> resolvedTypes =
                List.of(OrderDisputeStatus.RESOLVED, OrderDisputeStatus.CLOSED);
        if (vendorId == null) {
            open = orderDisputeRepository.countByStatus(OrderDisputeStatus.OPEN);
            pendingAdmin = orderDisputeRepository.countByStatus(OrderDisputeStatus.PENDING_ADMIN);
            resolvedCount = orderDisputeRepository.countByStatus(OrderDisputeStatus.RESOLVED)
                    + orderDisputeRepository.countByStatus(OrderDisputeStatus.CLOSED);
            metrics = orderDisputeRepository.findResolutionMetrics(resolvedTypes);
        } else {
            open = orderDisputeRepository.countByVendorAndStatus(vendorId, OrderDisputeStatus.OPEN);
            pendingAdmin = orderDisputeRepository.countByVendorAndStatus(vendorId, OrderDisputeStatus.PENDING_ADMIN);
            resolvedCount = orderDisputeRepository.countByVendorAndStatus(vendorId, OrderDisputeStatus.RESOLVED)
                    + orderDisputeRepository.countByVendorAndStatus(vendorId, OrderDisputeStatus.CLOSED);
            metrics = orderDisputeRepository.findResolutionMetricsByVendor(vendorId, resolvedTypes);
        }
        BigDecimal avgHours = calculateAverageResolutionHours(metrics);
        return new DisputeMetrics(open, pendingAdmin, resolvedCount, avgHours);
    }

    private BigDecimal calculateAverageResolutionHours(List<ResolutionMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalHours = BigDecimal.ZERO;
        int counted = 0;
        for (ResolutionMetric metric : metrics) {
            if (metric.getCreatedAt() == null || metric.getResolvedAt() == null) {
                continue;
            }
            long seconds = Duration.between(metric.getCreatedAt(), metric.getResolvedAt()).toSeconds();
            if (seconds < 0) {
                continue;
            }
            totalHours = totalHours.add(BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP));
            counted += 1;
        }
        if (counted == 0) {
            return BigDecimal.ZERO;
        }
        return totalHours.divide(BigDecimal.valueOf(counted), 2, RoundingMode.HALF_UP);
    }

    private SurveyMetrics buildSurveyMetrics(UUID vendorId) {
        long pending;
        long open;
        long completed;
        Double average;
        if (vendorId == null) {
            pending = surveyRepository.countByStatus(OrderSurveyStatus.PENDING);
            open = surveyRepository.countByStatus(OrderSurveyStatus.OPEN);
            completed = surveyRepository.countByStatus(OrderSurveyStatus.COMPLETED);
            average = surveyRepository.averageRating();
        } else {
            pending = surveyRepository.countByVendorAndStatus(vendorId, OrderSurveyStatus.PENDING);
            open = surveyRepository.countByVendorAndStatus(vendorId, OrderSurveyStatus.OPEN);
            completed = surveyRepository.countByVendorAndStatus(vendorId, OrderSurveyStatus.COMPLETED);
            average = surveyRepository.averageRatingByVendor(vendorId);
        }
        BigDecimal averageRating = average == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
        return new SurveyMetrics(pending, open, completed, averageRating);
    }

    private record TrendWindow(
            OffsetDateTime start,
            OffsetDateTime end,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
