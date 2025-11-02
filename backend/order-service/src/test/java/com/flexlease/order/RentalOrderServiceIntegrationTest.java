package com.flexlease.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.dto.AddCartItemRequest;
import com.flexlease.order.dto.CartItemResponse;
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
import com.flexlease.order.service.OrderMaintenanceScheduler;
import com.flexlease.order.service.RentalOrderService;
import com.flexlease.order.service.CartService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import com.flexlease.order.client.InventoryReservationClient;
import com.flexlease.order.client.NotificationClient;
import com.flexlease.order.client.PaymentClient;
import com.flexlease.order.client.PaymentStatus;
import com.flexlease.order.client.PaymentTransactionView;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
@TestPropertySource(properties = "flexlease.order.maintenance.pending-payment-expire-minutes=0")
class RentalOrderServiceIntegrationTest {

    static {
        System.setProperty("jdk.attach.allowAttachSelf", "true");
    }

    @Autowired
    private RentalOrderService rentalOrderService;

    @MockBean
    private PaymentClient paymentClient;

    @MockBean
    private NotificationClient notificationClient;

    @MockBean
    private InventoryReservationClient inventoryReservationClient;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderMaintenanceScheduler orderMaintenanceScheduler;

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
                List.of(itemRequest),
                List.of()
        ));
        assertThat(created.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        UUID transactionId = UUID.randomUUID();
        PaymentTransactionView transactionView = new PaymentTransactionView(
                transactionId,
                "P20241023",
                created.id(),
                userId,
                vendorId,
                PaymentStatus.SUCCEEDED,
                preview.totalAmount(),
                null,
                List.of()
        );
        org.mockito.Mockito.when(paymentClient.loadTransaction(transactionId)).thenReturn(transactionView);

        RentalOrderResponse paid = rentalOrderService.confirmPayment(created.id(),
                new OrderPaymentRequest(userId, transactionId.toString(), preview.totalAmount()));
        assertThat(paid.status()).isEqualTo(OrderStatus.AWAITING_SHIPMENT);
        assertThat(paid.paymentTransactionId()).isEqualTo(transactionId);

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

        org.mockito.Mockito.verify(inventoryReservationClient, org.mockito.Mockito.atLeastOnce())
                .outbound(org.mockito.ArgumentMatchers.eq(created.id()), org.mockito.ArgumentMatchers.anyList());
        org.mockito.Mockito.verify(inventoryReservationClient, org.mockito.Mockito.atLeastOnce())
                .release(org.mockito.ArgumentMatchers.eq(created.id()), org.mockito.ArgumentMatchers.anyList());
        org.mockito.Mockito.verify(inventoryReservationClient, org.mockito.Mockito.atLeastOnce())
                .inbound(org.mockito.ArgumentMatchers.eq(created.id()), org.mockito.ArgumentMatchers.anyList());
        org.mockito.Mockito.verify(paymentClient)
                .createRefund(org.mockito.ArgumentMatchers.eq(transactionId),
                        org.mockito.ArgumentMatchers.argThat(amount -> amount.compareTo(preview.depositAmount()) == 0),
                        org.mockito.ArgumentMatchers.anyString());

        // After completion, buyout should not be allowed; expect validation error
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                rentalOrderService.applyBuyout(created.id(), new OrderBuyoutApplyRequest(userId, BigDecimal.TEN, "买断")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void adminForceCloseCancelsOrder() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        OrderItemRequest itemRequest = new OrderItemRequest(
                productId,
                skuId,
                planId,
                "共享投影仪",
                "PROJ-001",
                null,
                1,
                new BigDecimal("199.00"),
                new BigDecimal("300.00"),
                null
        );

        RentalOrderResponse created = rentalOrderService.createOrder(new CreateOrderRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest),
                List.of()
        ));

        UUID transactionId = UUID.randomUUID();
        PaymentTransactionView transactionView = new PaymentTransactionView(
                transactionId,
                "P20241024",
                created.id(),
                userId,
                vendorId,
                PaymentStatus.SUCCEEDED,
                created.totalAmount(),
                null,
                List.of()
        );
        org.mockito.Mockito.when(paymentClient.loadTransaction(transactionId)).thenReturn(transactionView);
        rentalOrderService.confirmPayment(created.id(), new OrderPaymentRequest(userId, transactionId.toString(), created.totalAmount()));

                UUID adminId = UUID.randomUUID();
                FlexleasePrincipal principal = new FlexleasePrincipal(adminId, "admin-user", Set.of("ADMIN"));
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
                try {
                        RentalOrderResponse forced = rentalOrderService.forceClose(created.id(), "库存异常");
                        assertThat(forced.status()).isEqualTo(OrderStatus.CANCELLED);
                        assertThat(forced.events()).anyMatch(event -> event.description().contains("管理员强制关闭")
                                        || event.description().contains("库存异常"));
                } finally {
                        SecurityContextHolder.clearContext();
                }
    }

    @Test
    void shouldRejectPaymentWhenStatusNotSucceeded() {
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
                Map.of("termMonths", 6).toString(),
                1,
                new BigDecimal("299.00"),
                new BigDecimal("500.00"),
                null
        );

        RentalOrderResponse created = rentalOrderService.createOrder(new CreateOrderRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest),
                List.of()
        ));

        UUID transactionId = UUID.randomUUID();
        PaymentTransactionView transactionView = new PaymentTransactionView(
                transactionId,
                "P20241023",
                created.id(),
                userId,
                vendorId,
                PaymentStatus.PENDING,
                created.totalAmount(),
                null,
                List.of()
        );
        org.mockito.Mockito.when(paymentClient.loadTransaction(transactionId)).thenReturn(transactionView);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> rentalOrderService.confirmPayment(
                        created.id(),
                        new OrderPaymentRequest(userId, transactionId.toString(), created.totalAmount()))
        ).isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付尚未完成");

        RentalOrderResponse queried;
        try (SecurityContextHandle ignored = withPrincipal(userId, "user-%s".formatted(userId), "USER")) {
            queried = rentalOrderService.getOrder(created.id());
        }
        assertThat(queried.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void shouldCreateOrderUsingCartItems() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        CartItemResponse cartItem;
        try (SecurityContextHandle ignored = withPrincipal(userId, "user-%s".formatted(userId), "USER")) {
            cartItem = cartService.addItem(new AddCartItemRequest(
                    userId,
                    vendorId,
                    productId,
                    skuId,
                    planId,
                    "共享咖啡机",
                    "COFFEE-01",
                    null,
                    1,
                    new BigDecimal("99.00"),
                    new BigDecimal("200.00"),
                    null
            ));
        }

        RentalOrderResponse created;
        try (SecurityContextHandle ignored = withPrincipal(userId, "user-%s".formatted(userId), "USER")) {
            created = rentalOrderService.createOrder(new CreateOrderRequest(
                    userId,
                    vendorId,
                    "STANDARD",
                    null,
                    null,
                    List.of(),
                    List.of(cartItem.id())
            ));
        }

        assertThat(created.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        try (SecurityContextHandle ignored = withPrincipal(userId, "user-%s".formatted(userId), "USER")) {
            assertThat(cartService.listCartItems(userId)).isEmpty();
        }
    }

    @Test
    void schedulerCancelsExpiredOrders() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        OrderItemRequest itemRequest = new OrderItemRequest(
                productId,
                skuId,
                planId,
                "共享音箱",
                "SPEAKER-01",
                null,
                1,
                new BigDecimal("59.00"),
                new BigDecimal("100.00"),
                null
        );

        RentalOrderResponse created = rentalOrderService.createOrder(new CreateOrderRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest),
                List.of()
        ));

        orderMaintenanceScheduler.cancelExpiredPendingOrders();

        org.mockito.Mockito.verify(inventoryReservationClient, org.mockito.Mockito.atLeastOnce())
                .release(org.mockito.ArgumentMatchers.eq(created.id()), org.mockito.ArgumentMatchers.anyList());

                RentalOrderResponse refreshed;
                try (SecurityContextHandle ignored = withPrincipal(UUID.randomUUID(), "admin", "ADMIN")) {
                        refreshed = rentalOrderService.getOrder(created.id());
                }
                assertThat(refreshed.status()).isEqualTo(OrderStatus.CANCELLED);
    }

        private SecurityContextHandle withPrincipal(UUID userId, String username, String... roles) {
                Set<String> roleSet = roles.length == 0 ? Set.of() : Set.copyOf(List.of(roles));
                FlexleasePrincipal principal = new FlexleasePrincipal(userId, username, roleSet);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
                return new SecurityContextHandle();
        }

        private static final class SecurityContextHandle implements AutoCloseable {
                @Override
                public void close() {
                        SecurityContextHolder.clearContext();
                }
        }
}
