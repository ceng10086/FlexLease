package com.flexlease.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.user.CreditTier;
import com.flexlease.order.client.InventoryReservationClient;
import com.flexlease.order.client.NotificationClient;
import com.flexlease.order.client.PaymentClient;
import com.flexlease.order.client.ProductCatalogClient;
import com.flexlease.order.client.ProductCatalogClient.CatalogProductView;
import com.flexlease.order.client.ProductCatalogClient.CatalogProductView.RentalPlanView;
import com.flexlease.order.client.ProductCatalogClient.CatalogProductView.SkuView;
import com.flexlease.order.client.UserProfileClient;
import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderProofType;
import com.flexlease.order.dto.CreateOrderRequest;
import com.flexlease.order.dto.OrderItemRequest;
import com.flexlease.order.dto.OrderProofResponse;
import com.flexlease.order.service.OrderProofService;
import com.flexlease.order.service.RentalOrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "flexlease.proof-storage.root=target/test-order-proofs")
class OrderProofServiceIntegrationTest {

    @Autowired
    private RentalOrderService rentalOrderService;

    @Autowired
    private OrderProofService orderProofService;

    @MockBean
    private ProductCatalogClient productCatalogClient;

    @MockBean
    private InventoryReservationClient inventoryReservationClient;

    @MockBean
    private PaymentClient paymentClient;

    @MockBean
    private NotificationClient notificationClient;

    @MockBean
    private UserProfileClient userProfileClient;

    @BeforeEach
    void setupCredit() {
        Mockito.when(userProfileClient.loadCredit(Mockito.any()))
                .thenAnswer(invocation -> {
                    UUID userId = invocation.getArgument(0);
                    return new UserProfileClient.UserCreditView(userId, 70, CreditTier.STANDARD);
                });
    }

    @Test
    void vendorCanUploadShipmentProof() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID vendorUserId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        stubCatalog(productId, vendorId, planId, skuId);

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(
                userId,
                vendorId,
                "STANDARD",
                null,
                null,
                List.of(new OrderItemRequest(
                        productId,
                        skuId,
                        planId,
                        "凭证测试商品",
                        "SKU-PROOF",
                        null,
                        1,
                        new BigDecimal("188.00"),
                        new BigDecimal("288.00"),
                        null
                )),
                List.of()
        );
        var created = rentalOrderService.createOrder(createOrderRequest);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "proof.jpg",
                "image/jpeg",
                "demo".getBytes()
        );

        try (var ignored = withPrincipal(vendorUserId, vendorId, "vendor", "VENDOR")) {
            OrderProofResponse response = orderProofService.upload(
                    created.id(),
                    vendorUserId,
                    OrderProofType.SHIPMENT,
                    "包装外观",
                    file
            );
            assertThat(response.proofType()).isEqualTo(OrderProofType.SHIPMENT);
            assertThat(response.actorRole()).isEqualTo(OrderActorRole.VENDOR);
            assertThat(response.fileUrl()).startsWith("/proofs/");
            assertThat(orderProofService.list(created.id())).hasSize(1);
        }
    }

    private void stubCatalog(UUID productId, UUID vendorId, UUID planId, UUID skuId) {
        SkuView sku = new SkuView(skuId, "SKU-" + skuId.toString().substring(0, 4));
        RentalPlanView planView = new RentalPlanView(
                planId,
                "STANDARD",
                12,
                new BigDecimal("288.00"),
                new BigDecimal("188.00"),
                new BigDecimal("1999.00"),
                List.of(sku)
        );
        CatalogProductView productView = new CatalogProductView(productId, vendorId, null, List.of(planView));
        Mockito.when(productCatalogClient.getProduct(productId)).thenReturn(productView);
    }

    private SecurityContextHandle withPrincipal(UUID userId, UUID vendorId, String username, String... roles) {
        Set<String> roleSet = roles.length == 0 ? Set.of() : Set.copyOf(List.of(roles));
        FlexleasePrincipal principal = new FlexleasePrincipal(userId, vendorId, username, roleSet);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
        return new SecurityContextHandle();
    }

    private static final class SecurityContextHandle implements AutoCloseable {

        @Override
        public void close() {
            SecurityContextHolder.clearContext();
        }
    }
}
