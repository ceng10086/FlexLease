package com.flexlease.product.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.product.dto.InventoryReservationBatchRequest;
import com.flexlease.product.service.InventoryReservationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/inventory")
public class InternalInventoryController {

    private final InventoryReservationService reservationService;

    public InternalInventoryController(InventoryReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    public ApiResponse<Void> processReservations(@Valid @RequestBody InventoryReservationBatchRequest request) {
        reservationService.processReservations(request);
        return ApiResponse.success(null);
    }
}
