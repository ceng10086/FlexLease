package com.flexlease.user.service;

import com.flexlease.user.integration.OrderServiceClient;
import com.flexlease.user.integration.OrderServiceClient.VendorPerformanceMetrics;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SLA 评分计算服务。
 * <p>
 * 根据订单履约数据自动计算厂商 SLA 评分。
 * 计算公式：
 * <pre>
 * SLA = 准时发货率×40 + 纠纷解决率×30 + (100-取消率×100)×30
 * </pre>
 */
@Service
public class VendorSlaCalculationService {

    private static final Logger LOG = LoggerFactory.getLogger(VendorSlaCalculationService.class);

    private static final int ON_TIME_SHIPMENT_WEIGHT = 40;
    private static final int DISPUTE_RESOLUTION_WEIGHT = 30;
    private static final int LOW_CANCELLATION_WEIGHT = 30;

    private final OrderServiceClient orderServiceClient;

    public VendorSlaCalculationService(OrderServiceClient orderServiceClient) {
        this.orderServiceClient = orderServiceClient;
    }

    /**
     * 计算厂商的 SLA 评分（0-100）。
     *
     * @param vendorId 厂商 ID
     * @return SLA 评分，失败时返回 null
     */
    public Integer calculateSlaScore(UUID vendorId) {
        try {
            VendorPerformanceMetrics metrics = orderServiceClient.loadVendorPerformanceMetrics(vendorId);
            if (metrics == null) {
                LOG.warn("No performance metrics available for vendor {}", vendorId);
                return null;
            }

            // 准时发货率（0-100）
            double onTimeShipmentScore = metrics.onTimeShipmentRate() * 100;

            // 纠纷解决率（友好解决数 / 总纠纷数）
            double disputeResolutionScore = metrics.totalDisputes() > 0
                    ? (double) metrics.friendlyDisputes() / metrics.totalDisputes() * 100
                    : 100;

            // 低取消率得分（1 - 取消率）
            double lowCancellationScore = (1 - metrics.cancellationRate()) * 100;

            // 加权计算
            double sla = (onTimeShipmentScore * ON_TIME_SHIPMENT_WEIGHT
                    + disputeResolutionScore * DISPUTE_RESOLUTION_WEIGHT
                    + lowCancellationScore * LOW_CANCELLATION_WEIGHT) / 100;

            int finalScore = Math.max(0, Math.min(100, (int) Math.round(sla)));
            LOG.debug("Vendor {} SLA calculated: {} (onTime={}, dispute={}, cancel={})",
                    vendorId, finalScore, onTimeShipmentScore, disputeResolutionScore, lowCancellationScore);
            return finalScore;
        } catch (RuntimeException ex) {
            LOG.warn("Failed to calculate SLA for vendor {}: {}", vendorId, ex.getMessage());
            return null;
        }
    }
}
