package com.flexlease.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.flexlease.common.user.CreditTier;
import com.flexlease.order.domain.OrderEvent;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.repository.OrderEventRepository;
import com.flexlease.order.repository.RentalOrderRepository;
import com.flexlease.order.service.VendorPerformanceService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
class VendorPerformanceServiceTest {

    @Autowired
    private VendorPerformanceService vendorPerformanceService;

    @Autowired
    private RentalOrderRepository rentalOrderRepository;

    @Autowired
    private OrderEventRepository orderEventRepository;

    @Test
    void shouldCalculateOnTimeShipmentRateUsingPaymentWindow() {
        UUID vendorId = UUID.randomUUID();
        RentalOrder onTimeOrder = persistOrder(vendorId);
        OffsetDateTime paidAt = OffsetDateTime.now().minusDays(5);
        recordEvent(onTimeOrder, OrderEventType.PAYMENT_CONFIRMED, paidAt);
        recordEvent(onTimeOrder, OrderEventType.ORDER_SHIPPED, paidAt.plusHours(24));

        RentalOrder lateOrder = persistOrder(vendorId);
        OffsetDateTime latePaidAt = OffsetDateTime.now().minusDays(4);
        recordEvent(lateOrder, OrderEventType.PAYMENT_CONFIRMED, latePaidAt);
        recordEvent(lateOrder, OrderEventType.ORDER_SHIPPED, latePaidAt.plusHours(80));

        VendorPerformanceService.VendorPerformanceMetrics metrics = vendorPerformanceService.calculateMetrics(vendorId);
        assertThat(metrics.onTimeShipmentRate()).isEqualTo(0.5);
    }

    private RentalOrder persistOrder(UUID vendorId) {
        RentalOrder order = RentalOrder.create(
                UUID.randomUUID(),
                vendorId,
                "STANDARD",
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                null,
                new BigDecimal("150.00"),
                80,
                CreditTier.STANDARD,
                BigDecimal.ONE,
                false,
                null,
                null
        );
        return rentalOrderRepository.saveAndFlush(order);
    }

    private void recordEvent(RentalOrder order, OrderEventType type, OffsetDateTime createdAt) {
        OrderEvent event = OrderEvent.record(type, type.name(), order.getUserId());
        event.setOrder(order);
        OrderEvent persisted = orderEventRepository.saveAndFlush(event);
        ReflectionTestUtils.setField(persisted, "createdAt", createdAt);
        orderEventRepository.saveAndFlush(persisted);
    }
}
