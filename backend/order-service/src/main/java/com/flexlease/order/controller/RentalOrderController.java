package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.idempotency.IdempotencyService;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
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
import com.flexlease.order.dto.OrderInspectionRequest;
import com.flexlease.order.dto.OrderMessageRequest;
import com.flexlease.order.dto.OrderPaymentRequest;
import com.flexlease.order.dto.OrderPreviewRequest;
import com.flexlease.order.dto.OrderPreviewResponse;
import com.flexlease.order.dto.OrderReturnApplyRequest;
import com.flexlease.order.dto.OrderReturnCompleteRequest;
import com.flexlease.order.dto.OrderReturnDecisionRequest;
import com.flexlease.order.dto.OrderShipmentRequest;
import com.flexlease.order.dto.PagedResponse;
import com.flexlease.order.dto.RentalOrderResponse;
import com.flexlease.order.dto.RentalOrderSummaryResponse;
import com.flexlease.order.service.OrderContractService;
import com.flexlease.order.service.RentalOrderService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class RentalOrderController {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(10);

    private final RentalOrderService rentalOrderService;
    private final OrderContractService orderContractService;
    private final IdempotencyService idempotencyService;

    public RentalOrderController(RentalOrderService rentalOrderService,
                                 OrderContractService orderContractService,
                                 IdempotencyService idempotencyService) {
        this.rentalOrderService = rentalOrderService;
        this.orderContractService = orderContractService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/preview")
    public ApiResponse<OrderPreviewResponse> preview(@Valid @RequestBody OrderPreviewRequest request) {
        return ApiResponse.success(rentalOrderService.previewOrder(request));
    }

    @PostMapping
    public ApiResponse<RentalOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                                        @RequestHeader(value = "Idempotency-Key", required = false)
                                                        String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ApiResponse.success(rentalOrderService.createOrder(request));
        }
        String normalizedKey = idempotencyKey.trim();
        return idempotencyService.execute(
                "order:create:" + normalizedKey,
                IDEMPOTENCY_TTL,
                () -> ApiResponse.success(rentalOrderService.createOrder(request))
        );
    }

    @GetMapping
    public ApiResponse<PagedResponse<RentalOrderSummaryResponse>> listOrders(@RequestParam(required = false) UUID userId,
                                                                             @RequestParam(required = false) UUID vendorId,
                                                                             @RequestParam(required = false) OrderStatus status,
                                                                             @RequestParam(required = false) Boolean manualReviewOnly,
                                                                             @RequestParam(defaultValue = "1") int page,
                                                                             @RequestParam(defaultValue = "10") int size) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            if (userId != null && vendorId != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "userId 与 vendorId 不能同时提供");
            }
            Pageable adminPageable = PageRequest.of(Math.max(page - 1, 0), Math.max(1, Math.min(size, 100)),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
            return ApiResponse.success(rentalOrderService.listOrdersForAdmin(userId, vendorId, status, manualReviewOnly, adminPageable));
        }

        UUID effectiveUserId = null;
        UUID effectiveVendorId = null;

        if (principal.hasRole("VENDOR")) {
            UUID currentVendorId = principal.vendorId();
            if (currentVendorId == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少厂商标识");
            }
            if (vendorId != null && !vendorId.equals(currentVendorId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "禁止查看其他厂商的订单");
            }
            if (userId != null) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "厂商查询时无需指定 userId");
            }
            effectiveVendorId = currentVendorId;
        } else {
            UUID currentUserId = SecurityUtils.requireUserId();
            if (vendorId != null) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "仅厂商可按 vendorId 查询订单");
            }
            if (userId != null && !userId.equals(currentUserId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看其他用户的订单");
            }
            effectiveUserId = currentUserId;
        }

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        if (effectiveUserId != null) {
            return ApiResponse.success(rentalOrderService.listOrdersForUser(effectiveUserId, status, pageable));
        }
        if (effectiveVendorId != null) {
            return ApiResponse.success(rentalOrderService.listOrdersForVendor(effectiveVendorId, status, pageable));
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

    @PostMapping("/{orderId}/inspection/request")
    public ApiResponse<RentalOrderResponse> requestInspection(@PathVariable UUID orderId,
                                                              @Valid @RequestBody OrderInspectionRequest request) {
        return ApiResponse.success(rentalOrderService.requestInspection(orderId, request));
    }

    @PostMapping("/{orderId}/messages")
    public ApiResponse<RentalOrderResponse> postMessage(@PathVariable UUID orderId,
                                                        @Valid @RequestBody OrderMessageRequest request) {
        return ApiResponse.success(rentalOrderService.postConversationMessage(orderId, request));
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

    @PostMapping("/{orderId}/return/complete")
    public ApiResponse<RentalOrderResponse> completeReturn(@PathVariable UUID orderId,
                                                           @Valid @RequestBody OrderReturnCompleteRequest request) {
        return ApiResponse.success(rentalOrderService.completeReturn(orderId, request));
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
