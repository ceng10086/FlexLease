package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.order.dto.OrderDisputeResolveRequest;
import com.flexlease.order.dto.OrderDisputeResponse;
import com.flexlease.order.service.OrderDisputeService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders/{orderId}/disputes")
public class AdminOrderDisputeController {

    private final OrderDisputeService orderDisputeService;

    public AdminOrderDisputeController(OrderDisputeService orderDisputeService) {
        this.orderDisputeService = orderDisputeService;
    }

    @PostMapping("/{disputeId}/resolve")
    public ApiResponse<OrderDisputeResponse> resolve(@PathVariable UUID orderId,
                                                     @PathVariable UUID disputeId,
                                                     @Valid @RequestBody OrderDisputeResolveRequest request) {
        SecurityUtils.requireRole("ADMIN");
        return ApiResponse.success(orderDisputeService.resolve(orderId, disputeId, request));
    }
}
