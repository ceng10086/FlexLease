package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.dto.CreateOrderRequest;
import com.flexlease.order.dto.OrderActorRequest;
import com.flexlease.order.dto.OrderBuyoutApplyRequest;
import com.flexlease.order.dto.OrderBuyoutDecisionRequest;
import com.flexlease.order.dto.OrderCancelRequest;
import com.flexlease.order.dto.OrderContractResponse;
import com.flexlease.order.dto.OrderContractSignRequest;
import com.flexlease.order.dto.OrderExtensionApplyRequest;
import com.flexlease.order.dto.OrderExtensionDecisionRequest;
import com.flexlease.order.dto.OrderPaymentRequest;
import com.flexlease.order.dto.OrderPreviewRequest;
import com.flexlease.order.dto.OrderPreviewResponse;
import com.flexlease.order.dto.OrderReturnApplyRequest;
import com.flexlease.order.dto.OrderReturnDecisionRequest;
import com.flexlease.order.dto.OrderShipmentRequest;
import com.flexlease.order.dto.PagedResponse;
import com.flexlease.order.dto.RentalOrderResponse;
import com.flexlease.order.dto.RentalOrderSummaryResponse;
import com.flexlease.order.service.OrderContractService;
import com.flexlease.order.service.RentalOrderService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class RentalOrderController {

    private final RentalOrderService rentalOrderService;
    private final OrderContractService orderContractService;

    public RentalOrderController(RentalOrderService rentalOrderService,
                                 OrderContractService orderContractService) {
        this.rentalOrderService = rentalOrderService;
        this.orderContractService = orderContractService;
    }

    @PostMapping("/preview")
    public ApiResponse<OrderPreviewResponse> preview(@Valid @RequestBody OrderPreviewRequest request) {
        return ApiResponse.success(rentalOrderService.previewOrder(request));
    }

    @PostMapping
    public ApiResponse<RentalOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(rentalOrderService.createOrder(request));
    }

    @GetMapping
    public ApiResponse<PagedResponse<RentalOrderSummaryResponse>> listOrders(@RequestParam(required = false) UUID userId,
                                                                             @RequestParam(required = false) UUID vendorId,
                                                                             @RequestParam(required = false) OrderStatus status,
                                                                             @RequestParam(defaultValue = "1") int page,
                                                                             @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        if (userId != null && vendorId != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "userId 与 vendorId 不能同时提供");
        }
        if (userId != null) {
            return ApiResponse.success(rentalOrderService.listOrdersForUser(userId, status, pageable));
        }
        if (vendorId != null) {
            return ApiResponse.success(rentalOrderService.listOrdersForVendor(vendorId, status, pageable));
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请提供 userId 或 vendorId");
    }

    @GetMapping("/{orderId}")
    public ApiResponse<RentalOrderResponse> getOrder(@PathVariable UUID orderId) {
        return ApiResponse.success(rentalOrderService.getOrder(orderId));
    }

    @GetMapping("/{orderId}/contract")
    public ApiResponse<OrderContractResponse> getContract(@PathVariable UUID orderId) {
        return ApiResponse.success(orderContractService.getContract(orderId));
    }

    @PostMapping("/{orderId}/contract/sign")
    public ApiResponse<OrderContractResponse> signContract(@PathVariable UUID orderId,
                                                           @Valid @RequestBody OrderContractSignRequest request) {
        return ApiResponse.success(orderContractService.signContract(orderId, request));
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<RentalOrderResponse> confirmPayment(@PathVariable UUID orderId,
                                                           @Valid @RequestBody OrderPaymentRequest request) {
        return ApiResponse.success(rentalOrderService.confirmPayment(orderId, request));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<RentalOrderResponse> cancel(@PathVariable UUID orderId,
                                                   @Valid @RequestBody OrderCancelRequest request) {
        return ApiResponse.success(rentalOrderService.cancelOrder(orderId, request));
    }

    @PostMapping("/{orderId}/ship")
    public ApiResponse<RentalOrderResponse> ship(@PathVariable UUID orderId,
                                                 @Valid @RequestBody OrderShipmentRequest request) {
        return ApiResponse.success(rentalOrderService.shipOrder(orderId, request));
    }

    @PostMapping("/{orderId}/confirm-receive")
    public ApiResponse<RentalOrderResponse> confirmReceive(@PathVariable UUID orderId,
                                                           @Valid @RequestBody OrderActorRequest request) {
        return ApiResponse.success(rentalOrderService.confirmReceive(orderId, request));
    }

    @PostMapping("/{orderId}/extend")
    public ApiResponse<RentalOrderResponse> applyExtension(@PathVariable UUID orderId,
                                                           @Valid @RequestBody OrderExtensionApplyRequest request) {
        return ApiResponse.success(rentalOrderService.applyExtension(orderId, request));
    }

    @PostMapping("/{orderId}/extend/approve")
    public ApiResponse<RentalOrderResponse> decideExtension(@PathVariable UUID orderId,
                                                            @Valid @RequestBody OrderExtensionDecisionRequest request) {
        return ApiResponse.success(rentalOrderService.decideExtension(orderId, request));
    }

    @PostMapping("/{orderId}/return")
    public ApiResponse<RentalOrderResponse> applyReturn(@PathVariable UUID orderId,
                                                        @Valid @RequestBody OrderReturnApplyRequest request) {
        return ApiResponse.success(rentalOrderService.applyReturn(orderId, request));
    }

    @PostMapping("/{orderId}/return/approve")
    public ApiResponse<RentalOrderResponse> decideReturn(@PathVariable UUID orderId,
                                                         @Valid @RequestBody OrderReturnDecisionRequest request) {
        return ApiResponse.success(rentalOrderService.decideReturn(orderId, request));
    }

    @PostMapping("/{orderId}/buyout")
    public ApiResponse<RentalOrderResponse> applyBuyout(@PathVariable UUID orderId,
                                                        @Valid @RequestBody OrderBuyoutApplyRequest request) {
        return ApiResponse.success(rentalOrderService.applyBuyout(orderId, request));
    }

    @PostMapping("/{orderId}/buyout/confirm")
    public ApiResponse<RentalOrderResponse> decideBuyout(@PathVariable UUID orderId,
                                                         @Valid @RequestBody OrderBuyoutDecisionRequest request) {
        return ApiResponse.success(rentalOrderService.decideBuyout(orderId, request));
    }
}
