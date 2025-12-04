package com.flexlease.user.integration;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.user.config.OrderServiceProperties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 订单服务客户端。
 */
@Component
public class OrderServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(OrderServiceClient.class);

    private final RestClient restClient;

    public OrderServiceClient(RestClient.Builder builder, OrderServiceProperties properties) {
        this.restClient = builder
                .baseUrl(properties.getBaseUrl())
                .build();
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
     * 加载厂商履约指标。
     */
    public VendorPerformanceMetrics loadVendorPerformanceMetrics(UUID vendorId) {
        try {
            return restClient.get()
                    .uri("/api/v1/internal/vendors/{vendorId}/performance-metrics", vendorId)
                    .retrieve()
                    .body(VendorPerformanceMetrics.class);
        } catch (RestClientException ex) {
            LOG.warn("Failed to load vendor {} performance metrics: {}", vendorId, ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "获取厂商履约数据失败");
        }
    }
}
