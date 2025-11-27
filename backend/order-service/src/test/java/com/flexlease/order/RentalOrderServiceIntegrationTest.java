package com.flexlease.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.user.CreditTier;
import com.flexlease.order.client.InventoryReservationClient;
import com.flexlease.order.client.NotificationClient;
import com.flexlease.order.client.PaymentClient;
import com.flexlease.order.client.PaymentScene;
import com.flexlease.order.client.PaymentStatus;
import com.flexlease.order.client.PaymentTransactionView;
import com.flexlease.order.client.ProductCatalogClient;
import com.flexlease.order.client.ProductCatalogClient.CatalogProductView;
import com.flexlease.order.client.ProductCatalogClient.CatalogProductView.RentalPlanView;
import com.flexlease.order.client.ProductCatalogClient.CatalogProductView.SkuView;
import com.flexlease.order.client.UserProfileClient;
import com.flexlease.order.domain.DisputeResolutionOption;
import com.flexlease.order.domain.OrderDisputeStatus;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.OrderProofType;
import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.dto.AddCartItemRequest;
import com.flexlease.order.dto.CartItemResponse;
import com.flexlease.order.dto.CreateOrderRequest;
import com.flexlease.order.dto.OrderDisputeCreateRequest;
import com.flexlease.order.dto.OrderDisputeEscalateRequest;
import com.flexlease.order.dto.OrderDisputeResolveRequest;
import com.flexlease.order.dto.OrderDisputeResponse;
import com.flexlease.order.dto.OrderDisputeResponseRequest;
import com.flexlease.order.dto.OrderActorRequest;
import com.flexlease.order.dto.OrderBuyoutApplyRequest;
import com.flexlease.order.dto.OrderExtensionApplyRequest;
import com.flexlease.order.dto.OrderExtensionDecisionRequest;
import com.flexlease.order.dto.OrderItemRequest;
import com.flexlease.order.dto.OrderMessageRequest;
import com.flexlease.order.dto.OrderPaymentRequest;
import com.flexlease.order.dto.OrderPreviewRequest;
import com.flexlease.order.dto.OrderPreviewResponse;
import com.flexlease.order.dto.OrderReturnApplyRequest;
import com.flexlease.order.dto.OrderReturnCompleteRequest;
import com.flexlease.order.dto.OrderReturnDecisionRequest;
import com.flexlease.order.dto.OrderShipmentRequest;
import com.flexlease.order.dto.RentalOrderResponse;
import com.flexlease.order.service.CartService;
import com.flexlease.order.service.OrderContractService;
import com.flexlease.order.service.OrderDisputeService;
import com.flexlease.order.service.OrderMaintenanceScheduler;
import com.flexlease.order.service.OrderProofService;
import com.flexlease.order.service.RentalOrderService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@TestPropertySource(properties = {
        "flexlease.order.maintenance.pending-payment-expire-minutes=0",
        "flexlease.proof-storage.root=target/test-order-proofs"
})
class RentalOrderServiceIntegrationTest {

    static {
        System.setProperty("jdk.attach.allowAttachSelf", "true");
    }

    @Autowired
    private RentalOrderService rentalOrderService;

    @Autowired
    private OrderContractService orderContractService;

    @Autowired
    private OrderDisputeService orderDisputeService;

        @Autowired
        private OrderProofService orderProofService;

    @MockBean
    private PaymentClient paymentClient;

    @MockBean
    private NotificationClient notificationClient;

    @MockBean
    private InventoryReservationClient inventoryReservationClient;

    @MockBean
    private ProductCatalogClient productCatalogClient;

    @MockBean
    private UserProfileClient userProfileClient;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderMaintenanceScheduler orderMaintenanceScheduler;

    @BeforeEach
    void setupCreditClient() {
        Mockito.when(userProfileClient.loadCredit(Mockito.any()))
                .thenAnswer(invocation -> {
                    UUID userId = invocation.getArgument(0);
                    return new UserProfileClient.UserCreditView(userId, 60, CreditTier.STANDARD);
                });
        Mockito.doNothing().when(userProfileClient).adjustCredit(Mockito.any(), Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    void shouldCompleteFullOrderLifecycle() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
                UUID vendorAccountId = UUID.randomUUID();
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

        stubProductCatalog(productId, vendorId, planId, skuId);

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
        assertThat(preview.originalDepositAmount()).isEqualByComparingTo("1000.00");
        assertThat(preview.creditSnapshot().creditTier()).isEqualTo(CreditTier.STANDARD);

        RentalOrderResponse created = rentalOrderService.createOrder(new CreateOrderRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest),
                List.of(),
                        null
        ));
        assertThat(created.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        try (SecurityContextHandle ignored = withPrincipal(userId, "customer", "USER")) {
            RentalOrderResponse withMessage = rentalOrderService.postConversationMessage(created.id(),
                    new OrderMessageRequest(userId, "请尽快安排发货"));
            assertThat(withMessage.events())
                    .anyMatch(event -> event.eventType() == OrderEventType.COMMUNICATION_NOTE
                            && "请尽快安排发货".equals(event.description()));
        }

        UUID transactionId = UUID.randomUUID();
        PaymentTransactionView transactionView = new PaymentTransactionView(
                transactionId,
                "P20241023",
                created.id(),
                userId,
                vendorId,
                PaymentScene.DEPOSIT,
                PaymentStatus.SUCCEEDED,
                preview.totalAmount(),
                null,
                List.of(
                        new PaymentTransactionView.RefundView(
                                UUID.randomUUID(),
                                new BigDecimal("20.00"),
                                OffsetDateTime.now()
                        )
                )
        );
        org.mockito.Mockito.when(paymentClient.loadTransaction(transactionId)).thenReturn(transactionView);

        RentalOrderResponse paid = rentalOrderService.confirmPayment(created.id(),
                new OrderPaymentRequest(userId, transactionId.toString(), preview.totalAmount()));
        assertThat(paid.status()).isEqualTo(OrderStatus.AWAITING_SHIPMENT);
        assertThat(paid.paymentTransactionId()).isEqualTo(transactionId);

        uploadShipmentProofBundle(created.id(), vendorAccountId, vendorId);

        RentalOrderResponse shipped;
        try (SecurityContextHandle ignored = withPrincipal(vendorAccountId, vendorId, "vendor-%s".formatted(vendorId), "VENDOR")) {
            shipped = rentalOrderService.shipOrder(created.id(),
                    new OrderShipmentRequest(vendorId, "SF", "SF123456789"));
        }
        assertThat(shipped.status()).isEqualTo(OrderStatus.IN_LEASE);
        assertThat(shipped.shippingCarrier()).isEqualTo("SF");

        try (SecurityContextHandle ignored = withPrincipal(userId, "customer", "USER")) {
            MockMultipartFile receiveFile = new MockMultipartFile(
                    "file",
                    "receive.jpg",
                    "image/jpeg",
                    new byte[]{4, 5, 6}
            );
            orderProofService.upload(created.id(), userId, OrderProofType.RECEIVE, "收货取证", receiveFile);
        }

        RentalOrderResponse received = rentalOrderService.confirmReceive(created.id(),
                new OrderActorRequest(userId));
        assertThat(received.status()).isEqualTo(OrderStatus.IN_LEASE);

        RentalOrderResponse extensionRequested = rentalOrderService.applyExtension(created.id(),
                new OrderExtensionApplyRequest(userId, 3, "延长三个月"));
        assertThat(extensionRequested.extensionCount()).isZero();
        assertThat(extensionRequested.extensions()).hasSize(1);

        RentalOrderResponse extensionApproved;
        try (SecurityContextHandle ignored = withPrincipal(vendorAccountId, vendorId, "vendor-%s".formatted(vendorId), "VENDOR")) {
            extensionApproved = rentalOrderService.decideExtension(created.id(),
                    new OrderExtensionDecisionRequest(vendorId, true, "同意续租"));
        }
        assertThat(extensionApproved.extensionCount()).isEqualTo(1);
        assertThat(extensionApproved.extensions())
                .isNotEmpty();
        assertThat(extensionApproved.extensions()
                .get(extensionApproved.extensions().size() - 1)
                .status().name()).isEqualTo("APPROVED");

        RentalOrderResponse returnRequested = rentalOrderService.applyReturn(created.id(),
                new OrderReturnApplyRequest(userId, "不再需要", "SF", "SF987654321"));
        assertThat(returnRequested.status()).isEqualTo(OrderStatus.RETURN_REQUESTED);

        RentalOrderResponse returnApproved;
        try (SecurityContextHandle ignored = withPrincipal(vendorAccountId, vendorId, "vendor-%s".formatted(vendorId), "VENDOR")) {
            returnApproved = rentalOrderService.decideReturn(created.id(),
                    new OrderReturnDecisionRequest(vendorId, true, "已验收"));
        }
        assertThat(returnApproved.status()).isEqualTo(OrderStatus.RETURN_IN_PROGRESS);
        assertThat(returnApproved.returns())
                .isNotEmpty();
        assertThat(returnApproved.returns()
                .get(returnApproved.returns().size() - 1)
                .status().name()).isEqualTo("APPROVED");

        RentalOrderResponse returnCompleted;
        try (SecurityContextHandle ignored = withPrincipal(vendorAccountId, vendorId, "vendor-%s".formatted(vendorId), "VENDOR")) {
            returnCompleted = rentalOrderService.completeReturn(created.id(),
                    new OrderReturnCompleteRequest(vendorId, "确认完结"));
        }
        assertThat(returnCompleted.status()).isEqualTo(OrderStatus.COMPLETED);

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
    void shouldHandleDisputeLifecycle() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID vendorAccountId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        OrderItemRequest itemRequest = new OrderItemRequest(
                productId,
                skuId,
                planId,
                "纠纷测试商品",
                "SKU-DISPUTE",
                null,
                1,
                new BigDecimal("199.00"),
                new BigDecimal("399.00"),
                null
        );

        stubProductCatalog(productId, vendorId, planId, skuId);
        RentalOrderResponse created = rentalOrderService.createOrder(new CreateOrderRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest),
                List.of(),
                null
        ));

        OrderDisputeResponse opened;
        try (SecurityContextHandle ignored = withPrincipal(userId, "dispute-user", "USER")) {
            opened = orderDisputeService.create(created.id(), new OrderDisputeCreateRequest(
                    userId,
                    DisputeResolutionOption.PARTIAL_REFUND,
                    "设备存在划痕",
                    "请求折扣"
            ));
        }
        assertThat(opened.status()).isEqualTo(OrderDisputeStatus.OPEN);

        try (SecurityContextHandle ignored = withPrincipal(vendorAccountId, vendorId, "dispute-vendor", "VENDOR")) {
            orderDisputeService.respond(created.id(), opened.id(), new OrderDisputeResponseRequest(
                    vendorAccountId,
                    DisputeResolutionOption.REDELIVER,
                    false,
                    "优先考虑换新"
            ));
        }

        try (SecurityContextHandle ignored = withPrincipal(userId, "dispute-user", "USER")) {
            orderDisputeService.escalate(created.id(), opened.id(), new OrderDisputeEscalateRequest(
                    userId,
                    "希望平台仲裁"
            ));
        }

        OrderDisputeResponse resolved;
        UUID adminId = UUID.randomUUID();
        try (SecurityContextHandle ignored = withPrincipal(adminId, "admin", "ADMIN")) {
            resolved = orderDisputeService.resolve(created.id(), opened.id(), new OrderDisputeResolveRequest(
                    DisputeResolutionOption.PARTIAL_REFUND,
                    -12,
                    "确认用户责任"
            ));
        }

        assertThat(resolved.status()).isEqualTo(OrderDisputeStatus.CLOSED);
        assertThat(resolved.userCreditDelta()).isEqualTo(-12);
        Mockito.verify(userProfileClient).adjustCredit(Mockito.eq(userId), Mockito.eq(-12), Mockito.anyString());
    }

    @Test
    void shouldAllowInitiatorToRespondAfterCounterparty() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID vendorAccountId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        OrderItemRequest itemRequest = new OrderItemRequest(
                productId,
                skuId,
                planId,
                "纠纷轮次商品",
                "SKU-DISPUTE-RESP",
                null,
                1,
                new BigDecimal("120.00"),
                new BigDecimal("200.00"),
                null
        );

        stubProductCatalog(productId, vendorId, planId, skuId);
        RentalOrderResponse created = rentalOrderService.createOrder(new CreateOrderRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest),
                List.of(),
                null
        ));

        OrderDisputeResponse opened;
        try (SecurityContextHandle ignored = withPrincipal(userId, "user-dispute", "USER")) {
            opened = orderDisputeService.create(created.id(), new OrderDisputeCreateRequest(
                    userId,
                    DisputeResolutionOption.REDELIVER,
                    "配件缺失",
                    null
            ));
        }

        try (SecurityContextHandle ignored = withPrincipal(vendorAccountId, vendorId, "vendor-dispute", "VENDOR")) {
            orderDisputeService.respond(created.id(), opened.id(), new OrderDisputeResponseRequest(
                    vendorAccountId,
                    DisputeResolutionOption.PARTIAL_REFUND,
                    false,
                    "可提供折扣"
            ));
        }

        OrderDisputeResponse userResponse;
        try (SecurityContextHandle ignored = withPrincipal(userId, "user-dispute", "USER")) {
            userResponse = orderDisputeService.respond(created.id(), opened.id(), new OrderDisputeResponseRequest(
                    userId,
                    DisputeResolutionOption.PARTIAL_REFUND,
                    true,
                    "接受折扣"
            ));
        }
        assertThat(userResponse.status()).isEqualTo(OrderDisputeStatus.RESOLVED);

        try (SecurityContextHandle ignored = withPrincipal(userId, "user-dispute", "USER")) {
            assertThatThrownBy(() -> orderDisputeService.respond(created.id(), opened.id(), new OrderDisputeResponseRequest(
                    userId,
                    DisputeResolutionOption.RETURN_WITH_DEPOSIT_DEDUCTION,
                    true,
                    "重复提交"
            ))).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void shouldApplyCreditDiscount() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        Mockito.when(userProfileClient.loadCredit(Mockito.eq(userId)))
                .thenReturn(new UserProfileClient.UserCreditView(userId, 95, CreditTier.EXCELLENT));

        OrderItemRequest itemRequest = new OrderItemRequest(
                productId,
                skuId,
                planId,
                "信用减免商品",
                "CREDIT-01",
                Map.of("termMonths", 6).toString(),
                1,
                new BigDecimal("200.00"),
                new BigDecimal("300.00"),
                null
        );

        stubProductCatalog(productId, vendorId, planId, skuId);

        OrderPreviewResponse preview = rentalOrderService.previewOrder(new OrderPreviewRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest)
        ));

        assertThat(preview.originalDepositAmount()).isEqualByComparingTo("500.00");
        assertThat(preview.depositAmount()).isEqualByComparingTo("350.00");
        assertThat(preview.creditSnapshot().creditTier()).isEqualTo(CreditTier.EXCELLENT);
        assertThat(preview.creditSnapshot().depositAdjustmentRate()).isEqualByComparingTo("0.70");
    }

    @Test
    void shouldRejectOrderWhenCreditRestricted() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        Mockito.when(userProfileClient.loadCredit(Mockito.eq(userId)))
                .thenReturn(new UserProfileClient.UserCreditView(userId, 30, CreditTier.RESTRICTED));

        OrderItemRequest itemRequest = new OrderItemRequest(
                productId,
                skuId,
                planId,
                "信用校验商品",
                "CREDIT-02",
                null,
                1,
                new BigDecimal("120.00"),
                new BigDecimal("200.00"),
                null
        );

        stubProductCatalog(productId, vendorId, planId, skuId);

        assertThatThrownBy(() -> rentalOrderService.previewOrder(new OrderPreviewRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest)
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("信用");
    }

    @Test
    void shouldIgnoreClientManipulatedPricing() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        stubProductCatalog(productId, vendorId, planId, skuId);

        OrderItemRequest manipulatedItem = new OrderItemRequest(
                productId,
                skuId,
                planId,
                "测试商品",
                "SKU-X",
                null,
                1,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        OrderPreviewResponse preview = rentalOrderService.previewOrder(new OrderPreviewRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(manipulatedItem)
        ));

        assertThat(preview.depositAmount()).isEqualByComparingTo("500.00");
        assertThat(preview.rentAmount()).isEqualByComparingTo("299.00");
        assertThat(preview.totalAmount()).isEqualByComparingTo("799.00");

        RentalOrderResponse created = rentalOrderService.createOrder(new CreateOrderRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(manipulatedItem),
                List.of(),
                null
        ));

        assertThat(created.depositAmount()).isEqualByComparingTo("500.00");
        assertThat(created.rentAmount()).isEqualByComparingTo("299.00");
        assertThat(created.totalAmount()).isEqualByComparingTo("799.00");
        assertThat(created.items()).hasSize(1);
        assertThat(created.items().getFirst().unitDepositAmount()).isEqualByComparingTo("500.00");
        assertThat(created.items().getFirst().unitRentAmount()).isEqualByComparingTo("299.00");
    }

    @Test
        void adminForceCloseMarksExceptionStatus() {
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

        stubProductCatalog(productId, vendorId, planId, skuId);

        RentalOrderResponse created = rentalOrderService.createOrder(new CreateOrderRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest),
                List.of(),
                null
        ));

        UUID transactionId = UUID.randomUUID();
        PaymentTransactionView transactionView = new PaymentTransactionView(
                transactionId,
                "P20241024",
                created.id(),
                userId,
                vendorId,
                PaymentScene.DEPOSIT,
                PaymentStatus.SUCCEEDED,
                created.totalAmount(),
                null,
                List.of()
        );
        org.mockito.Mockito.when(paymentClient.loadTransaction(transactionId)).thenReturn(transactionView);
        rentalOrderService.confirmPayment(created.id(), new OrderPaymentRequest(userId, transactionId.toString(), created.totalAmount()));

                UUID adminId = UUID.randomUUID();
                FlexleasePrincipal principal = new FlexleasePrincipal(adminId, null, "admin-user", Set.of("ADMIN"));
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
                try {
                        RentalOrderResponse forced = rentalOrderService.forceClose(created.id(), "库存异常");
                        assertThat(forced.status()).isEqualTo(OrderStatus.EXCEPTION_CLOSED);
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

        stubProductCatalog(productId, vendorId, planId, skuId);

        RentalOrderResponse created = rentalOrderService.createOrder(new CreateOrderRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest),
                List.of(),
                null
        ));

        UUID transactionId = UUID.randomUUID();
        PaymentTransactionView transactionView = new PaymentTransactionView(
                transactionId,
                "P20241023",
                created.id(),
                userId,
                vendorId,
                PaymentScene.DEPOSIT,
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

        stubProductCatalog(productId, vendorId, planId, skuId);

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
                                        List.of(cartItem.id()),
                                        null
            ));
        }

        assertThat(created.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        try (SecurityContextHandle ignored = withPrincipal(userId, "user-%s".formatted(userId), "USER")) {
            assertThat(cartService.listCartItems(userId)).isEmpty();
        }
    }

    @Test
    void shouldHandlePaymentSuccessNotificationsIdempotently() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        stubProductCatalog(productId, vendorId, planId, skuId);

        RentalOrderResponse created;
        try (SecurityContextHandle ignored = withPrincipal(userId, "order-user", "USER")) {
            created = rentalOrderService.createOrder(new CreateOrderRequest(
                    userId,
                    vendorId,
                    "STANDARD",
                    null,
                    null,
                    List.of(new OrderItemRequest(
                            productId,
                            skuId,
                            planId,
                            "支付回调测试商品",
                            "SKU-PAY",
                            null,
                            1,
                            new BigDecimal("199.00"),
                            new BigDecimal("499.00"),
                            null
                    )),
                            List.of(),
                            null
            ));
        }

        UUID transactionId = UUID.randomUUID();
        PaymentTransactionView transactionView = new PaymentTransactionView(
                transactionId,
                "TX-" + transactionId.toString().substring(0, 6),
                created.id(),
                userId,
                vendorId,
                PaymentScene.DEPOSIT,
                PaymentStatus.SUCCEEDED,
                created.totalAmount(),
                OffsetDateTime.now(),
                List.of()
        );
        Mockito.when(paymentClient.loadTransaction(transactionId)).thenReturn(transactionView);

        RentalOrderResponse afterFirst = rentalOrderService.handlePaymentSuccess(created.id(), transactionId);
        assertThat(afterFirst.status()).isEqualTo(OrderStatus.AWAITING_SHIPMENT);
        assertThat(afterFirst.paymentTransactionId()).isEqualTo(transactionId);
        long paymentEventCount = afterFirst.events().stream()
                .filter(event -> event.eventType() == OrderEventType.PAYMENT_CONFIRMED)
                .count();
        assertThat(paymentEventCount).isEqualTo(1);

        Mockito.clearInvocations(notificationClient, paymentClient);

        RentalOrderResponse afterSecond = rentalOrderService.handlePaymentSuccess(created.id(), transactionId);
        assertThat(afterSecond.status()).isEqualTo(OrderStatus.AWAITING_SHIPMENT);
        assertThat(afterSecond.paymentTransactionId()).isEqualTo(transactionId);
        long paymentEventsAfterDuplicate = afterSecond.events().stream()
                .filter(event -> event.eventType() == OrderEventType.PAYMENT_CONFIRMED)
                .count();
        assertThat(paymentEventsAfterDuplicate).isEqualTo(1);
        Mockito.verify(notificationClient, Mockito.never()).send(Mockito.any());
        Mockito.verify(paymentClient, Mockito.never()).loadTransaction(transactionId);
    }

    @Test
    void shouldRecordSupplementalPaymentsAfterOrderSettled() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        stubProductCatalog(productId, vendorId, planId, skuId);

        RentalOrderResponse created;
        try (SecurityContextHandle ignored = withPrincipal(userId, "auto-pay-user", "USER")) {
            created = rentalOrderService.createOrder(new CreateOrderRequest(
                    userId,
                    vendorId,
                    "STANDARD",
                    null,
                    null,
                    List.of(new OrderItemRequest(
                            productId,
                            skuId,
                            planId,
                            "补款测试商品",
                            "SKU-TOPUP",
                            null,
                            1,
                            new BigDecimal("120.00"),
                            new BigDecimal("380.00"),
                            null
                    )),
                    List.of(),
                    null
            ));
        }

        UUID initialTransactionId = UUID.randomUUID();
        PaymentTransactionView initialTransaction = new PaymentTransactionView(
                initialTransactionId,
                "INIT-" + initialTransactionId.toString().substring(0, 6),
                created.id(),
                userId,
                vendorId,
                PaymentScene.DEPOSIT,
                PaymentStatus.SUCCEEDED,
                created.totalAmount(),
                OffsetDateTime.now(),
                List.of()
        );
        Mockito.when(paymentClient.loadTransaction(initialTransactionId)).thenReturn(initialTransaction);

        RentalOrderResponse paid = rentalOrderService.handlePaymentSuccess(created.id(), initialTransactionId);
        assertThat(paid.status()).isEqualTo(OrderStatus.AWAITING_SHIPMENT);

        UUID supplementalTransactionId = UUID.randomUUID();
        PaymentTransactionView supplementalTransaction = new PaymentTransactionView(
                supplementalTransactionId,
                "BUYOUT-" + supplementalTransactionId.toString().substring(0, 6),
                created.id(),
                userId,
                vendorId,
                PaymentScene.BUYOUT,
                PaymentStatus.SUCCEEDED,
                new BigDecimal("299.00"),
                OffsetDateTime.now(),
                List.of()
        );
        Mockito.when(paymentClient.loadTransaction(supplementalTransactionId)).thenReturn(supplementalTransaction);

        RentalOrderResponse afterSupplement = rentalOrderService.handlePaymentSuccess(created.id(), supplementalTransactionId);
        assertThat(afterSupplement.paymentTransactionId()).isEqualTo(initialTransactionId);
        assertThat(afterSupplement.events()).anyMatch(event ->
                event.eventType() == OrderEventType.ADDITIONAL_PAYMENT_RECORDED && event.description().contains("买断款"));
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

        stubProductCatalog(productId, vendorId, planId, skuId);

        RentalOrderResponse created = rentalOrderService.createOrder(new CreateOrderRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest),
                List.of(),
                null
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

    @Test
    void shouldRejectDirectOrderWhenVendorMismatch() {
        UUID userId = UUID.randomUUID();
        UUID requestedVendorId = UUID.randomUUID();
        UUID actualVendorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        stubProductCatalog(productId, actualVendorId, planId, skuId);

        OrderItemRequest itemRequest = new OrderItemRequest(
                productId,
                skuId,
                planId,
                "跨厂商商品",
                "SKU-001",
                null,
                1,
                new BigDecimal("120.00"),
                new BigDecimal("220.00"),
                null
        );

        assertThatThrownBy(() -> rentalOrderService.createOrder(new CreateOrderRequest(
                userId,
                requestedVendorId,
                "STANDARD",
                null,
                null,
                List.of(itemRequest),
                List.of(),
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void vendorCannotAccessOtherVendorContract() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID otherVendorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        stubProductCatalog(productId, vendorId, planId, skuId);

        RentalOrderResponse order = rentalOrderService.createOrder(new CreateOrderRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(new OrderItemRequest(
                        productId,
                        skuId,
                        planId,
                        "合同测试商品",
                        "SKU-C",
                        null,
                        1,
                        new BigDecimal("88.00"),
                        new BigDecimal("188.00"),
                        null
                )),
                List.of(),
                null
        ));

        try (SecurityContextHandle ignored = withPrincipal(UUID.randomUUID(), otherVendorId, "other-vendor", "VENDOR")) {
            assertThatThrownBy(() -> orderContractService.getContract(order.id()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权访问");
        }

        try (SecurityContextHandle ignored = withPrincipal(UUID.randomUUID(), vendorId, "current-vendor", "VENDOR")) {
            assertThat(orderContractService.getContract(order.id()).orderId()).isEqualTo(order.id());
        }
    }

    private void stubProductCatalog(UUID productId, UUID vendorId, UUID planId, UUID skuId) {
        UUID planIdentifier = planId != null ? planId : UUID.randomUUID();
        SkuView skuView = new SkuView(skuId, "SKU-" + skuId.toString().substring(0, 6));
        RentalPlanView planView = new RentalPlanView(
                planIdentifier,
                "STANDARD",
                12,
                new BigDecimal("500.00"),
                new BigDecimal("299.00"),
                new BigDecimal("2599.00"),
                List.of(skuView)
        );
        CatalogProductView catalogProductView = new CatalogProductView(productId, vendorId, null, List.of(planView));
        Mockito.when(productCatalogClient.getProduct(productId)).thenReturn(catalogProductView);
    }

        private void uploadShipmentProofBundle(UUID orderId, UUID vendorAccountId, UUID vendorId) {
                try (SecurityContextHandle ignored = withPrincipal(vendorAccountId, vendorId, "vendor-" + vendorId, "VENDOR")) {
                        for (int i = 0; i < 3; i++) {
                                MockMultipartFile photo = new MockMultipartFile(
                                                "file",
                                                "shipment-%d.jpg".formatted(i + 1),
                                                "image/jpeg",
                                                new byte[]{(byte) (i + 1)}
                                );
                                orderProofService.upload(orderId, vendorAccountId, OrderProofType.SHIPMENT, "发货照片" + (i + 1), photo);
                        }
                        MockMultipartFile video = new MockMultipartFile(
                                        "file",
                                        "evidence.mp4",
                                        "video/mp4",
                                        new byte[]{9, 9, 9}
                        );
                        orderProofService.upload(orderId, vendorAccountId, OrderProofType.SHIPMENT, "开机演示", video);
                }
        }

        private SecurityContextHandle withPrincipal(UUID userId, String username, String... roles) {
                return withPrincipal(userId, null, username, roles);
        }

        private SecurityContextHandle withPrincipal(UUID userId, UUID vendorId, String username, String... roles) {
                Set<String> roleSet = roles.length == 0 ? Set.of() : Set.copyOf(List.of(roles));
                FlexleasePrincipal principal = new FlexleasePrincipal(userId, vendorId, username, roleSet);
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
