package com.flexlease.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.user.CreditTier;
import com.flexlease.order.domain.DisputeResolutionOption;
import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderDispute;
import com.flexlease.order.domain.OrderEvent;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.OrderProof;
import com.flexlease.order.domain.OrderProofType;
import com.flexlease.order.domain.OrderSatisfactionSurvey;
import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.domain.RentalOrderItem;
import com.flexlease.order.dto.RentalOrderResponse;
import com.flexlease.order.repository.RentalOrderRepository;
import com.flexlease.order.service.RentalOrderService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
class RentalOrderServiceLazyLoadingTest {

    @Autowired
    private RentalOrderRepository rentalOrderRepository;

    @Autowired
    private RentalOrderService rentalOrderService;

    @Test
    void shouldLoadOrderDetailsWithoutLazyInitializationError() {
        RentalOrder order = createOrder();
        order.addEvent(OrderEvent.record(OrderEventType.ORDER_CREATED, "订单创建", order.getUserId(), OrderActorRole.USER));
        order.addProof(OrderProof.create(
                OrderProofType.SHIPMENT,
                "发货凭证",
                "proof.jpg",
                "/api/v1/proofs/proof.jpg",
                "image/jpeg",
                1024L,
                order.getVendorId(),
                OrderActorRole.VENDOR
        ));
        order.addDispute(OrderDispute.create(
                OrderActorRole.USER,
                order.getUserId(),
                DisputeResolutionOption.REDELIVER,
                "设备损坏",
                null
        ));
        order.addSurvey(OrderSatisfactionSurvey.create(
                OrderActorRole.USER,
                order.getUserId(),
                OffsetDateTime.now().plusHours(1)
        ));
        rentalOrderRepository.saveAndFlush(order);

                FlexleasePrincipal principal = new FlexleasePrincipal(order.getUserId(), order.getVendorId(), "tester", Set.of("USER"));
                TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null, "USER");
                SecurityContextHolder.getContext().setAuthentication(authentication);

                try {
                        RentalOrderResponse response = rentalOrderService.getOrder(order.getId());

                        assertThat(response.events()).hasSize(1);
                        assertThat(response.proofs()).hasSize(1);
                        assertThat(response.disputes()).hasSize(1);
                        assertThat(response.surveys()).hasSize(1);
                } finally {
                        SecurityContextHolder.clearContext();
                }
    }

    private RentalOrder createOrder() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        RentalOrder order = RentalOrder.create(
                userId,
                vendorId,
                "STANDARD",
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                null,
                new BigDecimal("300.00"),
                80,
                CreditTier.STANDARD,
                BigDecimal.ONE,
                false,
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusMonths(12)
        );
        RentalOrderItem item = RentalOrderItem.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "示例商品",
                "SKU-1",
                null,
                1,
                new BigDecimal("200.00"),
                new BigDecimal("100.00"),
                null
        );
        order.addItem(item);
        order.markPaid();
        order.ship("SF", "TRACK123");
        order.confirmReceive();
        return order;
    }
}
