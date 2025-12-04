package com.flexlease.order.controller;

import com.flexlease.order.service.VendorPerformanceService;
import com.flexlease.order.service.VendorPerformanceService.VendorPerformanceMetrics;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部 API 控制器，供其他微服务调用。
 */
@RestController
@RequestMapping("/api/v1/internal")
public class InternalOrderController {

    private final VendorPerformanceService performanceService;

    public InternalOrderController(VendorPerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    /**
     * 获取厂商履约指标（供 user-service 计算 SLA）。
     */
    @GetMapping("/vendors/{vendorId}/performance-metrics")
    public ResponseEntity<VendorPerformanceMetrics> getVendorPerformanceMetrics(
            @PathVariable UUID vendorId) {
        VendorPerformanceMetrics metrics = performanceService.calculateMetrics(vendorId);
        return ResponseEntity.ok(metrics);
    }
}
