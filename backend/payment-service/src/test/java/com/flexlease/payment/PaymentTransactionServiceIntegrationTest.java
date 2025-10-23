package com.flexlease.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.payment.domain.PaymentChannel;
import com.flexlease.payment.domain.PaymentScene;
import com.flexlease.payment.domain.PaymentStatus;
import com.flexlease.payment.domain.PaymentSplitType;
import com.flexlease.payment.domain.RefundStatus;
import com.flexlease.payment.dto.PaymentCallbackRequest;
import com.flexlease.payment.dto.PaymentConfirmRequest;
import com.flexlease.payment.dto.PaymentInitRequest;
import com.flexlease.payment.dto.PaymentRefundRequest;
import com.flexlease.payment.dto.PaymentSettlementResponse;
import com.flexlease.payment.dto.PaymentSplitRequest;
import com.flexlease.payment.dto.PaymentTransactionResponse;
import com.flexlease.payment.dto.RefundTransactionResponse;
import com.flexlease.payment.service.PaymentTransactionService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentTransactionServiceIntegrationTest {

    @Autowired
    private PaymentTransactionService paymentTransactionService;

    @Test
    void shouldInitConfirmRefundAndSettlePayment() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        PaymentSplitRequest platformSplit = new PaymentSplitRequest(
                PaymentSplitType.DEPOSIT_RESERVE,
                new BigDecimal("1000.00"),
                "PLATFORM_RESERVE"
        );
        PaymentSplitRequest vendorSplit = new PaymentSplitRequest(
                PaymentSplitType.VENDOR_INCOME,
                new BigDecimal("200.00"),
                "VENDOR_ACCOUNT"
        );

        PaymentTransactionResponse created = paymentTransactionService.initPayment(orderId, new PaymentInitRequest(
                userId,
                vendorId,
                PaymentScene.DEPOSIT,
                PaymentChannel.MOCK,
                new BigDecimal("1200.00"),
                "押金支付",
                List.of(platformSplit, vendorSplit)
        ));

        assertThat(created.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(created.splits()).hasSize(2);

        PaymentTransactionResponse confirmed = paymentTransactionService.confirmPayment(created.id(), new PaymentConfirmRequest(
                "MOCK-123456",
                OffsetDateTime.now()
        ));
        assertThat(confirmed.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(confirmed.paidAt()).isNotNull();

        RefundTransactionResponse refund = paymentTransactionService.createRefund(created.id(), new PaymentRefundRequest(
                new BigDecimal("200.00"),
                "部分退还"
        ));
        assertThat(refund.status()).isEqualTo(RefundStatus.SUCCEEDED);
        assertThat(refund.amount()).isEqualByComparingTo("200.00");

        List<PaymentSettlementResponse> settlements = paymentTransactionService.calculateSettlements(null, null, null, null, null);
        PaymentSettlementResponse settlement = settlements.stream()
                .filter(item -> item.vendorId().equals(vendorId))
                .findFirst()
                .orElseThrow();
        assertThat(settlement.vendorId()).isEqualTo(vendorId);
        assertThat(settlement.totalAmount()).isEqualByComparingTo("1200.00");
        assertThat(settlement.depositAmount()).isEqualByComparingTo("1200.00");
        assertThat(settlement.refundedAmount()).isEqualByComparingTo("200.00");
        assertThat(settlement.netAmount()).isEqualByComparingTo("1000.00");
        assertThat(settlement.transactionCount()).isEqualTo(1);
    }

    @Test
    void shouldRejectSplitAmountExceedingTotal() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        PaymentSplitRequest split = new PaymentSplitRequest(
                PaymentSplitType.VENDOR_INCOME,
                new BigDecimal("600.00"),
                "VENDOR_ACCOUNT"
        );

        PaymentInitRequest request = new PaymentInitRequest(
                userId,
                vendorId,
                PaymentScene.RENT,
                PaymentChannel.MOCK,
                new BigDecimal("500.00"),
                "租金支付",
                List.of(split)
        );

        assertThatThrownBy(() -> paymentTransactionService.initPayment(orderId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分账金额超过支付总额");
    }

    @Test
    void shouldHandleFailureCallback() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        PaymentTransactionResponse created = paymentTransactionService.initPayment(orderId, new PaymentInitRequest(
                userId,
                vendorId,
                PaymentScene.RENT,
                PaymentChannel.MOCK,
                new BigDecimal("800.00"),
                "租金",
                null
        ));

        PaymentTransactionResponse failed = paymentTransactionService.handleCallback(created.id(), new PaymentCallbackRequest(
                PaymentStatus.FAILED,
                "MOCK-FAILED-001",
                null,
                "余额不足"
        ));

        assertThat(failed.status()).isEqualTo(PaymentStatus.FAILED);

        assertThatThrownBy(() -> paymentTransactionService.confirmPayment(created.id(), new PaymentConfirmRequest("MOCK-123", OffsetDateTime.now())))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldFilterRefundsByTimeWindow() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        PaymentTransactionResponse created = paymentTransactionService.initPayment(orderId, new PaymentInitRequest(
                userId,
                vendorId,
                PaymentScene.RENT,
                PaymentChannel.MOCK,
                new BigDecimal("800.00"),
                "租金",
                null
        ));

        paymentTransactionService.confirmPayment(created.id(), new PaymentConfirmRequest(
                "MOCK-OK-01",
                OffsetDateTime.now()
        ));

        paymentTransactionService.createRefund(created.id(), new PaymentRefundRequest(
                new BigDecimal("300.00"),
                "尾款退还"
        ));

        OffsetDateTime future = OffsetDateTime.now().plusDays(1);
        List<PaymentSettlementResponse> settlements = paymentTransactionService.calculateSettlements(null, null, null, future, null);
        PaymentSettlementResponse settlement = settlements.stream()
                .filter(item -> item.vendorId().equals(vendorId))
                .findFirst()
                .orElseThrow();

        assertThat(settlement.refundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(settlement.netAmount()).isEqualByComparingTo("800.00");
    }
}
