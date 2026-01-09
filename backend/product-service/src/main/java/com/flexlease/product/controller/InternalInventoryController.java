package com.flexlease.product.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.product.dto.InventoryReservationBatchRequest;
import com.flexlease.product.service.InventoryReservationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存内部接口（供微服务之间调用）。
 * <p>
 * 典型调用方为 order-service：下单/取消/履约等场景需要预占/释放/出入库库存。
 */
@RestController
@RequestMapping("/api/v1/internal/inventory")
public class InternalInventoryController {

    private final InventoryReservationService reservationService;

    public InternalInventoryController(InventoryReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    public ApiResponse<Void> processReservations(@Valid @RequestBody InventoryReservationBatchRequest request) {
        SecurityUtils.requireRole("INTERNAL");
        reservationService.processReservations(request);
        return ApiResponse.success(null);
    }
}
