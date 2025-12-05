package com.flexlease.order.service;

import com.flexlease.order.domain.OrderDisputeStatus;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.repository.OrderDisputeRepository;
import com.flexlease.order.repository.OrderEventRepository;
import com.flexlease.order.repository.RentalOrderRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 厂商履约指标计算服务。
 * <p>
 * 提供厂商 SLA 评分所需的履约数据，包括：
 * <ul>
 *   <li>准时发货率</li>
 *   <li>纠纷数量与友好解决数</li>
 *   <li>取消率</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class VendorPerformanceService {

    private static final Duration ON_TIME_SHIPMENT_WINDOW = Duration.ofHours(48);

    private final RentalOrderRepository orderRepository;
    private final OrderDisputeRepository disputeRepository;
    private final OrderEventRepository orderEventRepository;

    public VendorPerformanceService(RentalOrderRepository orderRepository,
                                    OrderDisputeRepository disputeRepository,
                                    OrderEventRepository orderEventRepository) {
        this.orderRepository = orderRepository;
        this.disputeRepository = disputeRepository;
        this.orderEventRepository = orderEventRepository;
    }

    /**
     * 厂商履约指标。
     *
     * @param onTimeShipmentRate 准时发货率（0-1）
     * @param totalDisputes      总纠纷数
     * @param friendlyDisputes   友好解决纠纷数
     * @param cancellationRate   取消率（0-1）
     */
    public record VendorPerformanceMetrics(
            double onTimeShipmentRate,
            int totalDisputes,
            int friendlyDisputes,
            double cancellationRate
    ) {}

    /**
     * 计算厂商履约指标。
     */
    public VendorPerformanceMetrics calculateMetrics(UUID vendorId) {
        // 1. 计算准时发货率
        double onTimeShipmentRate = calculateOnTimeShipmentRate(vendorId);

        // 2. 统计纠纷数据
        long totalDisputes = disputeRepository.countByVendorAndStatus(vendorId, OrderDisputeStatus.CLOSED)
                + disputeRepository.countByVendorAndStatus(vendorId, OrderDisputeStatus.RESOLVED);
        long friendlyDisputes = disputeRepository.countByVendorAndStatus(vendorId, OrderDisputeStatus.RESOLVED);

        // 3. 计算取消率
        double cancellationRate = calculateCancellationRate(vendorId);

        return new VendorPerformanceMetrics(
                onTimeShipmentRate,
                (int) totalDisputes,
                (int) friendlyDisputes,
                cancellationRate
        );
    }

    private double calculateOnTimeShipmentRate(UUID vendorId) {
        var shipmentEvents = orderEventRepository.findEventTimestampsByVendorAndType(vendorId, OrderEventType.ORDER_SHIPPED);
        if (shipmentEvents.isEmpty()) {
            return 1.0;
        }
        Map<UUID, OffsetDateTime> paymentEvents = new HashMap<>();
        for (var payment : orderEventRepository.findEventTimestampsByVendorAndType(vendorId, OrderEventType.PAYMENT_CONFIRMED)) {
            paymentEvents.putIfAbsent(payment.getOrderId(), payment.getEventCreatedAt());
        }
        long onTime = shipmentEvents.stream()
                .filter(event -> isShipmentWithinWindow(event, paymentEvents))
                .count();
        return (double) onTime / shipmentEvents.size();
    }

    private boolean isShipmentWithinWindow(OrderEventRepository.EventTimestamp shipmentEvent,
                                           Map<UUID, OffsetDateTime> paymentEvents) {
        OffsetDateTime shippedAt = shipmentEvent.getEventCreatedAt();
        OffsetDateTime baseline = paymentEvents.getOrDefault(shipmentEvent.getOrderId(), shipmentEvent.getOrderCreatedAt());
        if (shippedAt == null || baseline == null) {
            return true;
        }
        Duration elapsed = Duration.between(baseline, shippedAt);
        return !elapsed.isNegative() && elapsed.compareTo(ON_TIME_SHIPMENT_WINDOW) <= 0;
    }

    private double calculateCancellationRate(UUID vendorId) {
        long cancelled = orderRepository.countByVendorIdAndStatus(vendorId, OrderStatus.CANCELLED);
        long total = orderRepository.countByVendorId(vendorId);
        if (total == 0) {
            return 0.0;
        }
        return (double) cancelled / total;
    }
}
