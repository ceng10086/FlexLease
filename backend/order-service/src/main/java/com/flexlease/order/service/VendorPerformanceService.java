package com.flexlease.order.service;

import com.flexlease.order.domain.OrderDisputeStatus;
import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.repository.OrderDisputeRepository;
import com.flexlease.order.repository.RentalOrderRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.UUID;
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

    private static final EnumSet<OrderStatus> COMPLETED_STATUSES = EnumSet.of(
            OrderStatus.COMPLETED,
            OrderStatus.BUYOUT_COMPLETED
    );

    private static final EnumSet<OrderStatus> CANCELLED_STATUS = EnumSet.of(OrderStatus.CANCELLED);

    private static final EnumSet<OrderDisputeStatus> RESOLVED_DISPUTE_STATUSES = EnumSet.of(
            OrderDisputeStatus.RESOLVED,
            OrderDisputeStatus.CLOSED
    );

    private static final Duration ON_TIME_SHIPMENT_WINDOW = Duration.ofHours(48);

    private final RentalOrderRepository orderRepository;
    private final OrderDisputeRepository disputeRepository;

    public VendorPerformanceService(RentalOrderRepository orderRepository,
                                    OrderDisputeRepository disputeRepository) {
        this.orderRepository = orderRepository;
        this.disputeRepository = disputeRepository;
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
        // 统计已完成订单中，在待发货状态时及时发货的比例
        // 简化实现：使用已完成订单数 / (已完成 + 已取消)
        long completed = orderRepository.countByVendorIdAndStatusIn(vendorId, COMPLETED_STATUSES);
        long total = orderRepository.countByVendorId(vendorId);
        if (total == 0) {
            return 1.0; // 无订单时默认满分
        }
        // 假设所有完成订单都是准时发货的（实际实现可能需要更复杂的逻辑）
        return Math.min(1.0, (double) completed / total);
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
