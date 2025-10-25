package com.flexlease.order.service;

import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.dto.DashboardMetricsResponse;
import com.flexlease.order.dto.VendorMetricsResponse;
import com.flexlease.order.repository.RentalOrderRepository;
import com.flexlease.order.repository.RentalOrderRepository.OrderStatusCount;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@Transactional(Transactional.TxType.SUPPORTS)
public class OrderAnalyticsService {

    private static final List<OrderStatus> ACTIVE_STATUSES = List.of(
            OrderStatus.AWAITING_SHIPMENT,
            OrderStatus.IN_LEASE,
            OrderStatus.RETURN_REQUESTED,
            OrderStatus.RETURN_IN_PROGRESS,
            OrderStatus.BUYOUT_REQUESTED
    );

    private static final List<OrderStatus> GMV_STATUSES = List.of(
            OrderStatus.AWAITING_SHIPMENT,
            OrderStatus.IN_LEASE,
            OrderStatus.RETURN_REQUESTED,
            OrderStatus.RETURN_IN_PROGRESS,
            OrderStatus.COMPLETED,
            OrderStatus.BUYOUT_REQUESTED,
            OrderStatus.BUYOUT_COMPLETED
    );

    private final RentalOrderRepository rentalOrderRepository;

    public OrderAnalyticsService(RentalOrderRepository rentalOrderRepository) {
        this.rentalOrderRepository = rentalOrderRepository;
    }

    public DashboardMetricsResponse getDashboardMetrics() {
        long totalOrders = rentalOrderRepository.count();
        long activeOrders = rentalOrderRepository.countByStatusIn(ACTIVE_STATUSES);
        long inLease = rentalOrderRepository.countByStatus(OrderStatus.IN_LEASE);
        long pendingReturns = rentalOrderRepository.countByStatus(OrderStatus.RETURN_REQUESTED);
        BigDecimal totalGmv = safeSum(rentalOrderRepository.sumTotalAmountByStatusIn(GMV_STATUSES));
        Map<OrderStatus, Long> statusMap = toStatusMap(rentalOrderRepository.aggregateStatus());
        return new DashboardMetricsResponse(totalOrders, activeOrders, totalGmv, inLease, pendingReturns, statusMap);
    }

    public VendorMetricsResponse getVendorMetrics(UUID vendorId) {
        long totalOrders = rentalOrderRepository.countByVendorId(vendorId);
        long activeOrders = rentalOrderRepository.countByVendorIdAndStatusIn(vendorId, ACTIVE_STATUSES);
        long inLease = rentalOrderRepository.countByVendorIdAndStatus(vendorId, OrderStatus.IN_LEASE);
        long pendingReturns = rentalOrderRepository.countByVendorIdAndStatus(vendorId, OrderStatus.RETURN_REQUESTED);
        BigDecimal totalGmv = safeSum(rentalOrderRepository.sumTotalAmountByVendorIdAndStatusIn(vendorId, GMV_STATUSES));
        Map<OrderStatus, Long> statusMap = toStatusMap(rentalOrderRepository.aggregateStatusByVendor(vendorId));
        return new VendorMetricsResponse(vendorId, totalOrders, activeOrders, totalGmv, inLease, pendingReturns, statusMap);
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
}
