package com.flexlease.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.user.CreditTier;
import com.flexlease.order.client.NotificationClient;
import com.flexlease.order.domain.DisputeResolutionOption;
import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderDispute;
import com.flexlease.order.domain.OrderSatisfactionSurvey;
import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.domain.RentalOrderItem;
import com.flexlease.order.dto.OrderSurveyResponse;
import com.flexlease.order.dto.OrderSurveySubmitRequest;
import com.flexlease.order.repository.OrderDisputeRepository;
import com.flexlease.order.repository.OrderSatisfactionSurveyRepository;
import com.flexlease.order.repository.RentalOrderRepository;
import com.flexlease.order.service.OrderSurveyService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "flexlease.order.survey.reminder-delay-hours=0",
        "flexlease.order.survey.activation-batch-size=5"
})
class OrderSurveyServiceIntegrationTest {

    @Autowired
    private OrderSurveyService orderSurveyService;

    @Autowired
    private RentalOrderRepository rentalOrderRepository;

    @Autowired
    private OrderSatisfactionSurveyRepository surveyRepository;

    @Autowired
    private OrderDisputeRepository orderDisputeRepository;

    @MockBean
    private NotificationClient notificationClient;

    private UUID userId;
    private UUID vendorId;

    @BeforeEach
    void setup() {
        Mockito.doNothing().when(notificationClient).send(Mockito.any());
        userId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldScheduleActivateAndSubmitSurvey() {
        RentalOrder order = createOrder();
        OrderDispute dispute = OrderDispute.create(
                OrderActorRole.USER,
                userId,
                DisputeResolutionOption.REDELIVER,
                "包装损坏",
                null
        );
        order.addDispute(dispute);
        rentalOrderRepository.save(order);
        RentalOrder managedOrder = rentalOrderRepository.findByIdWithDetails(order.getId()).orElseThrow();
        OrderDispute persistedDispute = orderDisputeRepository.findByIdAndOrderId(dispute.getId(), order.getId())
                .orElseThrow();
        order = managedOrder;

        orderSurveyService.scheduleForDispute(managedOrder, persistedDispute);
        List<OrderSatisfactionSurvey> scheduled = surveyRepository.findByOrderId(order.getId());
        assertThat(scheduled).hasSize(2);

        orderSurveyService.activatePendingSurveys();
        List<OrderSatisfactionSurvey> surveys = surveyRepository.findByOrderId(order.getId());
        UUID userSurveyId = surveys.stream()
                .filter(survey -> survey.getTargetRole() == OrderActorRole.USER)
                .map(OrderSatisfactionSurvey::getId)
                .findFirst()
                .orElseThrow();

        // 以用户身份提交问卷
        FlexleasePrincipal principal = new FlexleasePrincipal(
                userId,
                null,
                "user@test",
                Set.of("USER")
        );
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));

        OrderSurveyResponse submitted = orderSurveyService.submit(
                order.getId(),
                userSurveyId,
                new OrderSurveySubmitRequest(userId, 5, "非常满意")
        );

        assertThat(submitted.status()).isEqualTo(com.flexlease.order.domain.OrderSurveyStatus.COMPLETED);
        assertThat(submitted.rating()).isEqualTo(5);
        assertThat(submitted.comment()).isEqualTo("非常满意");
    }

    private RentalOrder createOrder() {
        RentalOrder order = RentalOrder.create(
                userId,
                vendorId,
                "STANDARD",
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                null,
                new BigDecimal("300.00"),
                70,
                CreditTier.STANDARD,
                BigDecimal.ONE,
                false,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusMonths(12)
        );
        RentalOrderItem item = RentalOrderItem.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "示例商品",
                "SKU",
                null,
                1,
                new BigDecimal("200.00"),
                new BigDecimal("100.00"),
                null
        );
        order.addItem(item);
        order.markPaid();
        order.ship("SF", "TRACK");
        return order;
    }
}
