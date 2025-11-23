package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.order.dto.OrderSurveyResponse;
import com.flexlease.order.dto.OrderSurveySubmitRequest;
import com.flexlease.order.service.OrderSurveyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/surveys")
public class OrderSurveyController {

    private final OrderSurveyService orderSurveyService;

    public OrderSurveyController(OrderSurveyService orderSurveyService) {
        this.orderSurveyService = orderSurveyService;
    }

    @GetMapping
    public ApiResponse<List<OrderSurveyResponse>> list(@PathVariable UUID orderId) {
        return ApiResponse.success(orderSurveyService.list(orderId));
    }

    @PostMapping("/{surveyId}/submit")
    public ApiResponse<OrderSurveyResponse> submit(@PathVariable UUID orderId,
                                                   @PathVariable UUID surveyId,
                                                   @Valid @RequestBody OrderSurveySubmitRequest request) {
        return ApiResponse.success(orderSurveyService.submit(orderId, surveyId, request));
    }
}
