package com.flexlease.payment.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.payment.dto.PaymentRefundRequest;
import com.flexlease.payment.dto.RefundTransactionResponse;
import com.flexlease.payment.service.PaymentTransactionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/payments")
public class InternalPaymentController {

    private final PaymentTransactionService paymentTransactionService;

    public InternalPaymentController(PaymentTransactionService paymentTransactionService) {
        this.paymentTransactionService = paymentTransactionService;
    }

    @PostMapping("/{transactionId}/refund")
    public ApiResponse<RefundTransactionResponse> refund(@PathVariable UUID transactionId,
                                                          @Valid @RequestBody PaymentRefundRequest request) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("INTERNAL")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅内部服务可访问该接口");
        }
        return ApiResponse.success(paymentTransactionService.createRefund(transactionId, request));
    }
}
