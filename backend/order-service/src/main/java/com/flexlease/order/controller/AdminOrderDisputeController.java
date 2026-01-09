package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.order.dto.DisputeAiSuggestionRequest;
import com.flexlease.order.dto.DisputeAiSuggestionResponse;
import com.flexlease.order.dto.OrderDisputeResolveRequest;
import com.flexlease.order.dto.OrderDisputeResponse;
import com.flexlease.order.service.DisputeAiSuggestionService;
import com.flexlease.order.service.OrderDisputeService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理侧纠纷接口：仲裁建议生成与平台裁决。
 * <p>
 * 角色校验在 Service 层完成：AI 建议仅 ARBITRATOR；裁决根据纠纷状态要求 ARBITRATOR 或 REVIEW_PANEL。
 */
@RestController
@RequestMapping("/api/v1/admin/orders/{orderId}/disputes")
public class AdminOrderDisputeController {

    private final OrderDisputeService orderDisputeService;
    private final DisputeAiSuggestionService disputeAiSuggestionService;

    public AdminOrderDisputeController(OrderDisputeService orderDisputeService,
                                       DisputeAiSuggestionService disputeAiSuggestionService) {
        this.orderDisputeService = orderDisputeService;
        this.disputeAiSuggestionService = disputeAiSuggestionService;
    }

    @PostMapping("/{disputeId}/ai-suggestion")
    public ApiResponse<DisputeAiSuggestionResponse> generateAiSuggestion(@PathVariable UUID orderId,
                                                                         @PathVariable UUID disputeId,
                                                                         @RequestBody(required = false)
                                                                         DisputeAiSuggestionRequest request) {
        return ApiResponse.success(disputeAiSuggestionService.generate(orderId, disputeId, request));
    }

    @PostMapping("/{disputeId}/resolve")
    public ApiResponse<OrderDisputeResponse> resolve(@PathVariable UUID orderId,
                                                     @PathVariable UUID disputeId,
                                                     @Valid @RequestBody OrderDisputeResolveRequest request) {
        return ApiResponse.success(orderDisputeService.resolve(orderId, disputeId, request));
    }
}
