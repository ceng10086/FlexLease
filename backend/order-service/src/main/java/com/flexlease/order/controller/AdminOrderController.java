package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.dto.OrderForceCloseRequest;
import com.flexlease.order.dto.PagedResponse;
import com.flexlease.order.dto.RentalOrderResponse;
import com.flexlease.order.dto.RentalOrderSummaryResponse;
import com.flexlease.order.service.RentalOrderService;
import jakarta.validation.Valid;
import java.util.Locale;
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
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final RentalOrderService rentalOrderService;

    public AdminOrderController(RentalOrderService rentalOrderService) {
        this.rentalOrderService = rentalOrderService;
    }

    @GetMapping
    public ApiResponse<PagedResponse<RentalOrderSummaryResponse>> listOrders(@RequestParam(required = false) String userId,
                                                                             @RequestParam(required = false) String vendorId,
                                                                             @RequestParam(required = false) String status,
                                                                             @RequestParam(required = false) Boolean manualReviewOnly,
                                                                             @RequestParam(defaultValue = "1") int page,
                                                                             @RequestParam(defaultValue = "10") int size) {
        UUID userUuid = parseUuid(userId, "userId");
        UUID vendorUuid = parseUuid(vendorId, "vendorId");
        OrderStatus statusEnum = parseStatus(status);
        SecurityUtils.requireAnyRole("ADMIN", "ARBITRATOR", "REVIEW_PANEL");
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(rentalOrderService.listOrdersForAdmin(userUuid, vendorUuid, statusEnum, manualReviewOnly, pageable));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<RentalOrderResponse> getOrder(@PathVariable UUID orderId) {
        SecurityUtils.requireAnyRole("ADMIN", "ARBITRATOR", "REVIEW_PANEL");
        return ApiResponse.success(rentalOrderService.getOrder(orderId));
    }

    @PostMapping("/{orderId}/force-close")
    public ApiResponse<RentalOrderResponse> forceClose(@PathVariable UUID orderId,
                                                       @Valid @RequestBody OrderForceCloseRequest request) {
        SecurityUtils.requireRole("ADMIN");
        return ApiResponse.success(rentalOrderService.forceClose(orderId, request.reason()));
    }

    private UUID parseUuid(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + " 需为合法 UUID");
        }
    }

    private OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法状态值: " + status);
        }
    }
}
