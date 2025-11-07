package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.order.dto.PaymentSuccessNotificationRequest;
import com.flexlease.order.dto.RentalOrderResponse;
import com.flexlease.order.service.RentalOrderService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/orders")
public class OrderInternalController {

    private final RentalOrderService rentalOrderService;

    public OrderInternalController(RentalOrderService rentalOrderService) {
        this.rentalOrderService = rentalOrderService;
    }

    @PostMapping("/{orderId}/payment-success")
    public ApiResponse<RentalOrderResponse> paymentSucceeded(@PathVariable UUID orderId,
                                                              @Valid @RequestBody PaymentSuccessNotificationRequest request) {
        SecurityUtils.requireRole("INTERNAL");
        return ApiResponse.success(rentalOrderService.handlePaymentSuccess(orderId, request.transactionId()));
    }
}
