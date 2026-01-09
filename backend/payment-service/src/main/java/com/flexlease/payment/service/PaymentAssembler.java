package com.flexlease.payment.service;

import com.flexlease.payment.domain.PaymentSplit;
import com.flexlease.payment.domain.PaymentSplitType;
import com.flexlease.payment.domain.PaymentTransaction;
import com.flexlease.payment.domain.RefundTransaction;
import com.flexlease.payment.dto.PaymentSplitResponse;
import com.flexlease.payment.dto.PaymentTransactionResponse;
import com.flexlease.payment.dto.RefundTransactionResponse;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 支付域对象组装器。
 *
 * <p>将 JPA 实体转换为对外 DTO，避免 Controller/Service 直接暴露实体结构。</p>
 */
@Component
public class PaymentAssembler {

    public PaymentTransactionResponse toResponse(PaymentTransaction transaction) {
        List<PaymentSplitResponse> splitResponses = transaction.getSplits().stream()
                .map(this::toSplitResponse)
                .toList();
        BigDecimal platformCommission = transaction.getSplits().stream()
                .filter(split -> split.getSplitType() == PaymentSplitType.PLATFORM_COMMISSION)
                .map(PaymentSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<RefundTransactionResponse> refundResponses = transaction.getRefunds().stream()
                .sorted(Comparator.comparing(RefundTransaction::getCreatedAt))
                .map(this::toRefundResponse)
                .toList();
        return new PaymentTransactionResponse(
                transaction.getId(),
                transaction.getTransactionNo(),
                transaction.getOrderId(),
                transaction.getUserId(),
                transaction.getVendorId(),
                transaction.getScene(),
                transaction.getStatus(),
                transaction.getChannel(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getChannelTransactionNo(),
                transaction.getPaidAt(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                transaction.getCommissionRate(),
                platformCommission,
                splitResponses,
                refundResponses
        );
    }

    private PaymentSplitResponse toSplitResponse(PaymentSplit split) {
        return new PaymentSplitResponse(
                split.getId(),
                split.getSplitType(),
                split.getAmount(),
                split.getBeneficiary()
        );
    }

    private RefundTransactionResponse toRefundResponse(RefundTransaction refund) {
        return new RefundTransactionResponse(
                refund.getId(),
                refund.getRefundNo(),
                refund.getStatus(),
                refund.getAmount(),
                refund.getReason(),
                refund.getRefundedAt(),
                refund.getCreatedAt(),
                refund.getUpdatedAt()
        );
    }
}
