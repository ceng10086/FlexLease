package com.flexlease.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.flexlease.payment.client.NotificationClient;
import com.flexlease.payment.client.OrderServiceClient;
import com.flexlease.payment.domain.PaymentChannel;
import com.flexlease.payment.domain.PaymentScene;
import com.flexlease.payment.domain.PaymentStatus;
import com.flexlease.payment.domain.RefundStatus;
import com.flexlease.payment.dto.PaymentCallbackRequest;
import com.flexlease.payment.dto.PaymentConfirmRequest;
import com.flexlease.payment.dto.PaymentInitRequest;
import com.flexlease.payment.dto.PaymentRefundRequest;
import com.flexlease.payment.dto.PaymentTransactionResponse;
import com.flexlease.payment.dto.RefundTransactionResponse;
import com.flexlease.payment.service.PaymentTransactionService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "flexlease.payment.auto-confirm=false")
@ActiveProfiles("test")
class PaymentTransactionServiceManualFlowTest {

    private static final String H2_JDBC_URL =
            "jdbc:h2:mem:flexlease-payment-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS payment";

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_JDBC_URL);
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired
    private PaymentTransactionService paymentTransactionService;

    @MockBean
    private NotificationClient notificationClient;

    @MockBean
    private OrderServiceClient orderServiceClient;

    @Test
    void shouldSupportManualConfirmationAndRefundFlow() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        PaymentTransactionResponse created = paymentTransactionService.initPayment(orderId, new PaymentInitRequest(
                userId,
                vendorId,
                PaymentScene.DEPOSIT,
                PaymentChannel.MOCK,
                new BigDecimal("1200.00"),
                "押金",
                null
        ));

        assertThat(created.status()).isEqualTo(PaymentStatus.PENDING);

        PaymentTransactionResponse confirmed = paymentTransactionService.confirmPayment(created.id(), new PaymentConfirmRequest(
                "MOCK-CHANNEL-01",
                OffsetDateTime.now()
        ));
        assertThat(confirmed.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(orderServiceClient).notifyPaymentSucceeded(created.orderId(), created.id());

        RefundTransactionResponse refund = paymentTransactionService.createRefund(created.id(), new PaymentRefundRequest(
                new BigDecimal("200.00"),
                "部分退还"
        ));
        assertThat(refund.status()).isEqualTo(RefundStatus.SUCCEEDED);
    }

    @Test
    void shouldHandleFailureCallbackWhenPending() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        PaymentTransactionResponse created = paymentTransactionService.initPayment(orderId, new PaymentInitRequest(
                userId,
                vendorId,
                PaymentScene.RENT,
                PaymentChannel.MOCK,
                new BigDecimal("500.00"),
                "租金",
                null
        ));
        assertThat(created.status()).isEqualTo(PaymentStatus.PENDING);

        PaymentTransactionResponse failed = paymentTransactionService.handleCallback(created.id(), new PaymentCallbackRequest(
                PaymentStatus.FAILED,
                "MOCK-FAIL-01",
                null,
                "余额不足"
        ));

        assertThat(failed.status()).isEqualTo(PaymentStatus.FAILED);
        verify(orderServiceClient, never()).notifyPaymentSucceeded(any(), any());
    }
}
