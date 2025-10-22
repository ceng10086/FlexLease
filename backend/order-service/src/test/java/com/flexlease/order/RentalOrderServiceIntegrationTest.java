package com.flexlease.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.dto.CreateOrderRequest;
import com.flexlease.order.dto.OrderActorRequest;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.order.dto.OrderBuyoutApplyRequest;
import com.flexlease.order.dto.OrderExtensionApplyRequest;
import com.flexlease.order.dto.OrderExtensionDecisionRequest;
import com.flexlease.order.dto.OrderItemRequest;
import com.flexlease.order.dto.OrderPaymentRequest;
import com.flexlease.order.dto.OrderPreviewRequest;
import com.flexlease.order.dto.OrderReturnApplyRequest;
import com.flexlease.order.dto.OrderReturnDecisionRequest;
import com.flexlease.order.dto.OrderShipmentRequest;
import com.flexlease.order.dto.OrderPreviewResponse;
import com.flexlease.order.dto.RentalOrderResponse;
import com.flexlease.order.service.RentalOrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RentalOrderServiceIntegrationTest {

    @Autowired
    private RentalOrderService rentalOrderService;

    @Test
    void shouldCompleteFullOrderLifecycle() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        OrderItemRequest itemRequest = new OrderItemRequest(
                productId,
                skuId,
                planId,
                "智能共享办公桌",
                "DESK-001",
                Map.of("termMonths", 12).toString(),
                2,
                new BigDecimal("299.00"),
                new BigDecimal("500.00"),
                new BigDecimal("2599.00")
        );

        OrderPreviewResponse preview = rentalOrderService.previewOrder(new OrderPreviewRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest)
        ));

        assertThat(preview.depositAmount()).isEqualByComparingTo("1000.00");
        assertThat(preview.rentAmount()).isEqualByComparingTo("598.00");
        assertThat(preview.totalAmount()).isEqualByComparingTo("1598.00");

        RentalOrderResponse created = rentalOrderService.createOrder(new CreateOrderRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest)
        ));
        assertThat(created.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        RentalOrderResponse paid = rentalOrderService.confirmPayment(created.id(),
                new OrderPaymentRequest(userId, "PAY-123456", preview.totalAmount()));
        assertThat(paid.status()).isEqualTo(OrderStatus.AWAITING_SHIPMENT);

        RentalOrderResponse shipped = rentalOrderService.shipOrder(created.id(),
                new OrderShipmentRequest(vendorId, "SF", "SF123456789"));
        assertThat(shipped.status()).isEqualTo(OrderStatus.IN_LEASE);
        assertThat(shipped.shippingCarrier()).isEqualTo("SF");

        RentalOrderResponse received = rentalOrderService.confirmReceive(created.id(),
                new OrderActorRequest(userId));
        assertThat(received.status()).isEqualTo(OrderStatus.IN_LEASE);

        RentalOrderResponse extensionRequested = rentalOrderService.applyExtension(created.id(),
                new OrderExtensionApplyRequest(userId, 3, "延长三个月"));
        assertThat(extensionRequested.extensionCount()).isZero();
        assertThat(extensionRequested.extensions()).hasSize(1);

        RentalOrderResponse extensionApproved = rentalOrderService.decideExtension(created.id(),
                new OrderExtensionDecisionRequest(vendorId, true, "同意续租"));
        assertThat(extensionApproved.extensionCount()).isEqualTo(1);
        assertThat(extensionApproved.extensions())
                .isNotEmpty();
        assertThat(extensionApproved.extensions()
                .get(extensionApproved.extensions().size() - 1)
                .status().name()).isEqualTo("APPROVED");

        RentalOrderResponse returnRequested = rentalOrderService.applyReturn(created.id(),
                new OrderReturnApplyRequest(userId, "不再需要", "SF", "SF987654321"));
        assertThat(returnRequested.status()).isEqualTo(OrderStatus.RETURN_REQUESTED);

        RentalOrderResponse returnApproved = rentalOrderService.decideReturn(created.id(),
                new OrderReturnDecisionRequest(vendorId, true, "已验收"));
        assertThat(returnApproved.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(returnApproved.returns())
                .isNotEmpty();
        assertThat(returnApproved.returns()
                .get(returnApproved.returns().size() - 1)
                .status().name()).isEqualTo("APPROVED");

        // After completion, buyout should not be allowed; expect validation error
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                rentalOrderService.applyBuyout(created.id(), new OrderBuyoutApplyRequest(userId, BigDecimal.TEN, "买断")))
                .isInstanceOf(BusinessException.class);
    }
}
