package com.flexlease.payment.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.payment.dto.PaymentCallbackRequest;
import com.flexlease.payment.dto.PaymentConfirmRequest;
import com.flexlease.payment.dto.PaymentInitRequest;
import com.flexlease.payment.dto.PaymentRefundRequest;
import com.flexlease.payment.dto.PaymentSettlementResponse;
import com.flexlease.payment.dto.PaymentTransactionResponse;
import com.flexlease.payment.dto.RefundTransactionResponse;
import com.flexlease.payment.service.PaymentTransactionService;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentTransactionService paymentTransactionService;

    public PaymentController(PaymentTransactionService paymentTransactionService) {
        this.paymentTransactionService = paymentTransactionService;
    }

    @PostMapping("/{orderId}/init")
    public ApiResponse<PaymentTransactionResponse> initPayment(@PathVariable UUID orderId,
                                                               @Valid @RequestBody PaymentInitRequest request) {
        return ApiResponse.success(paymentTransactionService.initPayment(orderId, request));
    }

    @GetMapping("/{transactionId}")
    public ApiResponse<PaymentTransactionResponse> getTransaction(@PathVariable UUID transactionId) {
        return ApiResponse.success(paymentTransactionService.getTransaction(transactionId));
    }

    @PostMapping("/{transactionId}/confirm")
    public ApiResponse<PaymentTransactionResponse> confirm(@PathVariable UUID transactionId,
                                                            @Valid @RequestBody PaymentConfirmRequest request) {
        return ApiResponse.success(paymentTransactionService.confirmPayment(transactionId, request));
    }

    @PostMapping("/{transactionId}/callback")
    public ApiResponse<PaymentTransactionResponse> handleCallback(@PathVariable UUID transactionId,
                                                                   @Valid @RequestBody PaymentCallbackRequest request) {
        return ApiResponse.success(paymentTransactionService.handleCallback(transactionId, request));
    }

    @PostMapping("/{transactionId}/refund")
    public ApiResponse<RefundTransactionResponse> refund(@PathVariable UUID transactionId,
                                                         @Valid @RequestBody PaymentRefundRequest request) {
        return ApiResponse.success(paymentTransactionService.createRefund(transactionId, request));
    }

    @GetMapping("/settlements")
    public ApiResponse<List<PaymentSettlementResponse>> settlements(@RequestParam(required = false) UUID vendorId,
                                                                    @RequestParam(required = false)
                                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
                                                                    @RequestParam(required = false)
                                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
                                                                    @RequestParam(required = false)
                                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime refundFrom,
                                                                    @RequestParam(required = false)
                                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime refundTo) {
        return ApiResponse.success(paymentTransactionService.calculateSettlements(vendorId, from, to, refundFrom, refundTo));
    }
}
