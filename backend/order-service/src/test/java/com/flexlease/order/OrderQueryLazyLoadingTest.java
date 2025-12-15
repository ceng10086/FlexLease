package com.flexlease.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.user.CreditTier;
import com.flexlease.order.domain.DisputeResolutionOption;
import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderDispute;
import com.flexlease.order.domain.OrderProof;
import com.flexlease.order.domain.OrderProofType;
import com.flexlease.order.domain.OrderSatisfactionSurvey;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.domain.RentalOrderItem;
import com.flexlease.order.dto.OrderContractResponse;
import com.flexlease.order.dto.PagedResponse;
import com.flexlease.order.repository.RentalOrderRepository;
import com.flexlease.order.service.OrderContractService;
import com.flexlease.order.service.OrderDisputeService;
import com.flexlease.order.service.OrderProofService;
import com.flexlease.order.service.OrderSurveyService;
import com.flexlease.order.service.RentalOrderService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
class OrderQueryLazyLoadingTest {

    @Autowired
    private RentalOrderRepository rentalOrderRepository;

    @Autowired
    private RentalOrderService rentalOrderService;

    @Autowired
    private OrderProofService orderProofService;

    @Autowired
    private OrderDisputeService orderDisputeService;

    @Autowired
    private OrderSurveyService orderSurveyService;

    @Autowired
    private OrderContractService orderContractService;

    @Test
    void shouldLoadListingsAndDisputeResourcesWithinTransaction() {
        RentalOrder order = createOrder();
        order.addProof(OrderProof.create(
                OrderProofType.SHIPMENT,
                "物流凭证",
                "proof.jpg",
            "/api/v1/proofs/proof.jpg",
                "image/jpeg",
                2048L,
                order.getVendorId(),
                OrderActorRole.VENDOR
        ));
        OrderDispute dispute = OrderDispute.create(
                OrderActorRole.USER,
                order.getUserId(),
                DisputeResolutionOption.REDELIVER,
                "设备有划痕",
                null
        );
        order.addDispute(dispute);
        OrderSatisfactionSurvey survey = OrderSatisfactionSurvey.create(
                OrderActorRole.USER,
                order.getUserId(),
                OffsetDateTime.now().plusHours(1)
        );
        survey.setDispute(dispute);
        order.addSurvey(survey);
        rentalOrderRepository.saveAndFlush(order);

        FlexleasePrincipal userPrincipal = new FlexleasePrincipal(order.getUserId(), order.getVendorId(), "user", Set.of("USER"));
        setAuthentication(userPrincipal);
        try {
            assertThat(orderProofService.list(order.getId())).hasSize(1);
            assertThat(orderDisputeService.list(order.getId())).hasSize(1);
            assertThat(orderSurveyService.list(order.getId())).hasSize(1);
            PagedResponse<?> userOrders = rentalOrderService.listOrdersForUser(order.getUserId(), null, PageRequest.of(0, 5));
            assertThat(userOrders.content()).hasSize(1);
        } finally {
            SecurityContextHolder.clearContext();
        }

        FlexleasePrincipal vendorPrincipal = new FlexleasePrincipal(null, order.getVendorId(), "vendor", Set.of("VENDOR"));
        setAuthentication(vendorPrincipal);
        try {
            PagedResponse<?> vendorOrders = rentalOrderService.listOrdersForVendor(order.getVendorId(), null, PageRequest.of(0, 5));
            assertThat(vendorOrders.content()).hasSize(1);
        } finally {
            SecurityContextHolder.clearContext();
        }

        FlexleasePrincipal adminPrincipal = new FlexleasePrincipal(UUID.randomUUID(), null, "admin", Set.of("ADMIN"));
        setAuthentication(adminPrincipal);
        try {
            PagedResponse<?> adminOrders = rentalOrderService.listOrdersForAdmin(null, null, null, null, PageRequest.of(0, 5));
            assertThat(adminOrders.content()).isNotEmpty();
        } finally {
            SecurityContextHolder.clearContext();
        }

        FlexleasePrincipal contractPrincipal = new FlexleasePrincipal(order.getUserId(), order.getVendorId(), "user", Set.of("USER"));
        setAuthentication(contractPrincipal);
        try {
            OrderContractResponse contract = orderContractService.getContract(order.getId());
            assertThat(contract.orderId()).isEqualTo(order.getId());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void setAuthentication(FlexleasePrincipal principal) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                principal,
                null,
                principal.roles().toArray(new String[0])
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private RentalOrder createOrder() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        RentalOrder order = RentalOrder.create(
                userId,
                vendorId,
                "STANDARD",
                new BigDecimal("150.00"),
                new BigDecimal("150.00"),
                new BigDecimal("200.00"),
                null,
                new BigDecimal("350.00"),
                85,
                CreditTier.EXCELLENT,
                BigDecimal.ONE,
                false,
                OffsetDateTime.now().minusDays(2),
                OffsetDateTime.now().plusMonths(6)
        );
        RentalOrderItem item = RentalOrderItem.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "测试商品",
                "SKU-123",
                null,
                1,
                new BigDecimal("200.00"),
                new BigDecimal("150.00"),
                null
        );
        order.addItem(item);
        order.markPaid();
        order.ship("SF", "TRACK123");
        order.confirmReceive();
        return order;
    }
}
