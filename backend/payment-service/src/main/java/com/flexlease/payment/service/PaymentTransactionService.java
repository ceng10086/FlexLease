package com.flexlease.payment.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.payment.domain.PaymentScene;
import com.flexlease.payment.domain.PaymentSplit;
import com.flexlease.payment.domain.PaymentStatus;
import com.flexlease.payment.domain.PaymentTransaction;
import com.flexlease.payment.domain.RefundStatus;
import com.flexlease.payment.domain.RefundTransaction;
import com.flexlease.payment.dto.PaymentCallbackRequest;
import com.flexlease.payment.dto.PaymentConfirmRequest;
import com.flexlease.payment.dto.PaymentInitRequest;
import com.flexlease.payment.dto.PaymentRefundRequest;
import com.flexlease.payment.dto.PaymentSettlementResponse;
import com.flexlease.payment.dto.PaymentSplitRequest;
import com.flexlease.payment.dto.PaymentTransactionResponse;
import com.flexlease.payment.dto.RefundTransactionResponse;
import com.flexlease.payment.repository.PaymentTransactionRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentAssembler assembler;

    public PaymentTransactionService(PaymentTransactionRepository paymentTransactionRepository,
                                     PaymentAssembler assembler) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.assembler = assembler;
    }

    public PaymentTransactionResponse initPayment(UUID orderId, PaymentInitRequest request) {
        paymentTransactionRepository.findFirstByOrderIdAndSceneAndStatus(orderId, request.scene(), PaymentStatus.PENDING)
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "存在待支付的同类流水");
                });

        BigDecimal splitSum = request.splits() == null ? BigDecimal.ZERO : request.splits().stream()
                .map(PaymentSplitRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (splitSum.compareTo(request.amount()) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "分账金额超过支付总额");
        }

        try {
            PaymentTransaction transaction = PaymentTransaction.create(
                    orderId,
                    request.userId(),
                    request.vendorId(),
                    request.scene(),
                    request.channel(),
                    request.amount(),
                    request.description()
            );
            if (request.splits() != null) {
                request.splits().forEach(splitRequest ->
                        transaction.addSplit(PaymentSplit.create(splitRequest.splitType(),
                                splitRequest.amount(),
                                splitRequest.beneficiary())));
            }
            PaymentTransaction saved = paymentTransactionRepository.save(transaction);
            return assembler.toResponse(saved);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PaymentTransactionResponse getTransaction(UUID transactionId) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "支付流水不存在"));
        return assembler.toResponse(transaction);
    }

    public PaymentTransactionResponse confirmPayment(UUID transactionId, PaymentConfirmRequest request) {
        PaymentTransaction transaction = getTransactionForUpdate(transactionId);
        try {
            transaction.markSucceeded(request.channelTransactionNo(), request.paidAt());
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
        return assembler.toResponse(transaction);
    }

    public PaymentTransactionResponse handleCallback(UUID transactionId, PaymentCallbackRequest request) {
        if (request.status() == PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "回调状态不合法");
        }
        PaymentTransaction transaction = getTransactionForUpdate(transactionId);
        try {
            if (request.status() == PaymentStatus.SUCCEEDED) {
                OffsetDateTime paidAt = request.paidAt() != null ? request.paidAt() : OffsetDateTime.now();
                transaction.markSucceeded(request.channelTransactionNo(), paidAt);
            } else {
                transaction.markFailed(request.channelTransactionNo());
            }
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
        return assembler.toResponse(transaction);
    }

    public RefundTransactionResponse createRefund(UUID transactionId, PaymentRefundRequest request) {
        PaymentTransaction transaction = getTransactionForUpdate(transactionId);
        try {
            RefundTransaction refund = transaction.createRefund(request.amount(), request.reason());
            // 模拟通道立即退款成功
            refund.markSucceeded();
            return assembler.toResponse(transaction).refunds().stream()
                    .filter(r -> r.id().equals(refund.getId()))
                    .findFirst()
                    .orElseThrow();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
    }

    public List<PaymentSettlementResponse> calculateSettlements(UUID vendorId,
                                                                 OffsetDateTime from,
                                                                 OffsetDateTime to,
                                                                 OffsetDateTime refundFrom,
                                                                 OffsetDateTime refundTo) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "开始时间不能晚于结束时间");
        }
        if (refundFrom != null && refundTo != null && refundFrom.isAfter(refundTo)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "退款开始时间不能晚于结束时间");
        }
        Specification<PaymentTransaction> specification = (root, query, cb) -> {
            var predicates = cb.conjunction();
            predicates.getExpressions().add(cb.equal(root.get("status"), PaymentStatus.SUCCEEDED));
            if (vendorId != null) {
                predicates.getExpressions().add(cb.equal(root.get("vendorId"), vendorId));
            }
            if (from != null) {
                predicates.getExpressions().add(cb.greaterThanOrEqualTo(root.get("paidAt"), from));
            }
            if (to != null) {
                predicates.getExpressions().add(cb.lessThanOrEqualTo(root.get("paidAt"), to));
            }
            return predicates;
        };
        List<PaymentTransaction> transactions = paymentTransactionRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "paidAt"));
        Map<UUID, SettlementAccumulator> grouped = new LinkedHashMap<>();
        for (PaymentTransaction transaction : transactions) {
            SettlementAccumulator accumulator = grouped.computeIfAbsent(transaction.getVendorId(), key -> new SettlementAccumulator(refundFrom, refundTo));
            accumulator.add(transaction);
        }
        return grouped.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue().toResponse(entry.getKey()))
                .toList();
    }

    private PaymentTransaction getTransactionForUpdate(UUID transactionId) {
        return paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "支付流水不存在"));
    }

    private static class SettlementAccumulator {
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal deposit = BigDecimal.ZERO;
        private BigDecimal rent = BigDecimal.ZERO;
        private BigDecimal buyout = BigDecimal.ZERO;
        private BigDecimal penalty = BigDecimal.ZERO;
        private BigDecimal refunded = BigDecimal.ZERO;
        private OffsetDateTime lastPaidAt;
        private long count;
        private final OffsetDateTime refundFrom;
        private final OffsetDateTime refundTo;

        SettlementAccumulator(OffsetDateTime refundFrom, OffsetDateTime refundTo) {
            this.refundFrom = refundFrom;
            this.refundTo = refundTo;
        }

        void add(PaymentTransaction transaction) {
            total = total.add(transaction.getAmount());
            switch (transaction.getScene()) {
                case DEPOSIT -> deposit = deposit.add(transaction.getAmount());
                case RENT -> rent = rent.add(transaction.getAmount());
                case BUYOUT -> buyout = buyout.add(transaction.getAmount());
                case PENALTY -> penalty = penalty.add(transaction.getAmount());
            }
            OffsetDateTime paidAt = transaction.getPaidAt() != null ? transaction.getPaidAt() : transaction.getUpdatedAt();
            if (lastPaidAt == null || paidAt.isAfter(lastPaidAt)) {
                lastPaidAt = paidAt;
            }
            count += 1;
            transaction.getRefunds().stream()
                    .filter(refund -> refund.getStatus() == RefundStatus.SUCCEEDED)
                    .filter(refund -> isWithinRefundWindow(refund.getRefundedAt()))
                    .forEach(refund -> refunded = refunded.add(refund.getAmount()));
        }

        PaymentSettlementResponse toResponse(UUID vendorId) {
            BigDecimal netAmount = total.subtract(refunded);
            return new PaymentSettlementResponse(vendorId, total, deposit, rent, buyout, penalty, refunded, netAmount, lastPaidAt, count);
        }

        private boolean isWithinRefundWindow(OffsetDateTime refundedAt) {
            if (refundedAt == null) {
                return false;
            }
            if (refundFrom != null && refundedAt.isBefore(refundFrom)) {
                return false;
            }
            if (refundTo != null && refundedAt.isAfter(refundTo)) {
                return false;
            }
            return true;
        }
    }
}
