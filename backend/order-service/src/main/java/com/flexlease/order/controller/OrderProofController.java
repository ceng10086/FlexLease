package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.order.domain.OrderProofType;
import com.flexlease.order.dto.OrderProofResponse;
import com.flexlease.order.service.OrderProofService;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 订单取证接口：上传与查询取证材料。
 * <p>
 * 权限与可上传类型限制在 {@link com.flexlease.order.service.OrderProofService} 中统一校验。
 */
@RestController
@RequestMapping("/api/v1/orders/{orderId}/proofs")
public class OrderProofController {

    private final OrderProofService orderProofService;

    public OrderProofController(OrderProofService orderProofService) {
        this.orderProofService = orderProofService;
    }

    @GetMapping
    public ApiResponse<List<OrderProofResponse>> list(@PathVariable UUID orderId) {
        return ApiResponse.success(orderProofService.list(orderId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OrderProofResponse> upload(@PathVariable UUID orderId,
                                                  @RequestParam("actorId") @NotNull UUID actorId,
                                                  @RequestParam("proofType") OrderProofType proofType,
                                                  @RequestParam(value = "description", required = false) String description,
                                                  @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(orderProofService.upload(orderId, actorId, proofType, description, file));
    }
}
