package com.flexlease.payment.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.idempotency.IdempotencyService;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.payment.dto.PaymentCallbackRequest;
import com.flexlease.payment.dto.PaymentConfirmRequest;
import com.flexlease.payment.dto.PaymentInitRequest;
import com.flexlease.payment.dto.PaymentRefundRequest;
import com.flexlease.payment.dto.PaymentSettlementResponse;
import com.flexlease.payment.dto.PaymentTransactionResponse;
import com.flexlease.payment.dto.RefundTransactionResponse;
import com.flexlease.payment.service.PaymentTransactionService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(10);

    private final PaymentTransactionService paymentTransactionService;
    private final IdempotencyService idempotencyService;

    public PaymentController(PaymentTransactionService paymentTransactionService,
                             IdempotencyService idempotencyService) {
        this.paymentTransactionService = paymentTransactionService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/{orderId}/init")
    public ApiResponse<PaymentTransactionResponse> initPayment(@PathVariable UUID orderId,
                                                               @Valid @RequestBody PaymentInitRequest request,
                                                               @RequestHeader(value = "Idempotency-Key", required = false)
                                                               String idempotencyKey) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("ADMIN") && !principal.hasRole("INTERNAL")) {
            if (!principal.hasRole("USER")) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "仅消费者可创建支付");
            }
            if (principal.userId() == null || !principal.userId().equals(request.userId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "禁止使用他人账号创建支付");
            }
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ApiResponse.success(paymentTransactionService.initPayment(orderId, request));
        }
        String normalizedKey = idempotencyKey.trim();
        return idempotencyService.execute(
                "payment:init:" + normalizedKey,
                IDEMPOTENCY_TTL,
                () -> ApiResponse.success(paymentTransactionService.initPayment(orderId, request))
        );
    }

    @GetMapping("/{transactionId}")
    public ApiResponse<PaymentTransactionResponse> getTransaction(@PathVariable UUID transactionId) {
        SecurityUtils.requirePrincipal();
        return ApiResponse.success(paymentTransactionService.getTransaction(transactionId));
    }

    @PostMapping("/{transactionId}/confirm")
    public ApiResponse<PaymentTransactionResponse> confirm(@PathVariable UUID transactionId,
                                                            @Valid @RequestBody PaymentConfirmRequest request) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("ADMIN") && !principal.hasRole("INTERNAL")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可确认支付");
        }
        return ApiResponse.success(paymentTransactionService.confirmPayment(transactionId, request));
    }

    @PostMapping("/{transactionId}/callback")
    public ApiResponse<PaymentTransactionResponse> handleCallback(@PathVariable UUID transactionId,
                                                                   @Valid @RequestBody PaymentCallbackRequest request) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("INTERNAL") && !principal.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅内部调用可触发回调");
        }
        return ApiResponse.success(paymentTransactionService.handleCallback(transactionId, request));
    }

    @PostMapping("/{transactionId}/refund")
    public ApiResponse<RefundTransactionResponse> refund(@PathVariable UUID transactionId,
                                                         @Valid @RequestBody PaymentRefundRequest request) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可发起退款");
        }
        return ApiResponse.success(paymentTransactionService.createRefund(transactionId, request));
    }

    @GetMapping("/settlements")
    public ApiResponse<List<PaymentSettlementResponse>> settlements(@RequestParam(value = "vendorId", required = false) UUID vendorId,
                                                                    @RequestParam(value = "from", required = false)
                                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
                                                                    @RequestParam(value = "to", required = false)
                                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
                                                                    @RequestParam(value = "refundFrom", required = false)
                                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime refundFrom,
                                                                    @RequestParam(value = "refundTo", required = false)
                                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime refundTo) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        UUID effectiveVendorId = vendorId;
        if (principal.hasRole("ADMIN")) {
            // 管理员可查询任意厂商；vendorId 为空表示查询全量
        } else if (principal.hasRole("VENDOR")) {
            if (principal.vendorId() == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少厂商标识");
            }
            if (vendorId != null && !principal.vendorId().equals(vendorId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "禁止查询其他厂商结算");
            }
            effectiveVendorId = principal.vendorId();
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN, "缺少访问结算数据的权限");
        }
        return ApiResponse.success(paymentTransactionService.calculateSettlements(effectiveVendorId, from, to, refundFrom, refundTo));
    }
}
