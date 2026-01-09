package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.order.dto.OrderDisputeAppealRequest;
import com.flexlease.order.dto.OrderDisputeCreateRequest;
import com.flexlease.order.dto.OrderDisputeEscalateRequest;
import com.flexlease.order.dto.OrderDisputeResponse;
import com.flexlease.order.dto.OrderDisputeResponseRequest;
import com.flexlease.order.service.OrderDisputeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单纠纷接口（用户/厂商侧）。
 * <p>
 * 纠纷的操作人需同时满足：请求体 actorId 与当前 JWT 用户一致，并且拥有该订单的访问权限。
 */
@RestController
@RequestMapping("/api/v1/orders/{orderId}/disputes")
public class OrderDisputeController {

    private final OrderDisputeService orderDisputeService;

    public OrderDisputeController(OrderDisputeService orderDisputeService) {
        this.orderDisputeService = orderDisputeService;
    }

    @GetMapping
    public ApiResponse<List<OrderDisputeResponse>> list(@PathVariable UUID orderId) {
        return ApiResponse.success(orderDisputeService.list(orderId));
    }

    @PostMapping
    public ApiResponse<OrderDisputeResponse> create(@PathVariable UUID orderId,
                                                    @Valid @RequestBody OrderDisputeCreateRequest request) {
        return ApiResponse.success(orderDisputeService.create(orderId, request));
    }

    @PostMapping("/{disputeId}/responses")
    public ApiResponse<OrderDisputeResponse> respond(@PathVariable UUID orderId,
                                                     @PathVariable UUID disputeId,
                                                     @Valid @RequestBody OrderDisputeResponseRequest request) {
        return ApiResponse.success(orderDisputeService.respond(orderId, disputeId, request));
    }

    @PostMapping("/{disputeId}/escalate")
    public ApiResponse<OrderDisputeResponse> escalate(@PathVariable UUID orderId,
                                                      @PathVariable UUID disputeId,
                                                      @Valid @RequestBody OrderDisputeEscalateRequest request) {
        return ApiResponse.success(orderDisputeService.escalate(orderId, disputeId, request));
    }

    @PostMapping("/{disputeId}/appeal")
    public ApiResponse<OrderDisputeResponse> appeal(@PathVariable UUID orderId,
                                                    @PathVariable UUID disputeId,
                                                    @Valid @RequestBody OrderDisputeAppealRequest request) {
        return ApiResponse.success(orderDisputeService.appeal(orderId, disputeId, request));
    }

}
