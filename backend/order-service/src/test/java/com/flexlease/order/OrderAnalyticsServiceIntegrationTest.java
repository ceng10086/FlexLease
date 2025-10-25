package com.flexlease.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.domain.RentalOrderItem;
import com.flexlease.order.dto.DashboardMetricsResponse;
import com.flexlease.order.dto.VendorMetricsResponse;
import com.flexlease.order.repository.RentalOrderRepository;
import com.flexlease.order.service.OrderAnalyticsService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderAnalyticsServiceIntegrationTest {

    @Autowired
    private RentalOrderRepository rentalOrderRepository;

    @Autowired
    private OrderAnalyticsService orderAnalyticsService;

    private UUID userId;
    private UUID vendorId;

    @BeforeEach
    void setUp() {
        rentalOrderRepository.deleteAll();
        userId = UUID.randomUUID();
        vendorId = UUID.randomUUID();

        RentalOrder awaitingShipment = createOrder(OrderStatus.AWAITING_SHIPMENT, new BigDecimal("100.00"));
        rentalOrderRepository.save(awaitingShipment);

        RentalOrder inLease = createOrder(OrderStatus.IN_LEASE, new BigDecimal("200.00"));
        rentalOrderRepository.save(inLease);

        RentalOrder returnRequested = createOrder(OrderStatus.RETURN_REQUESTED, new BigDecimal("300.00"));
        rentalOrderRepository.save(returnRequested);

        RentalOrder cancelled = createOrder(OrderStatus.PENDING_PAYMENT, new BigDecimal("150.00"));
        cancelled.cancel();
        rentalOrderRepository.save(cancelled);
    }

    @Test
    void shouldCalculateDashboardMetrics() {
        DashboardMetricsResponse response = orderAnalyticsService.getDashboardMetrics();
        assertThat(response.totalOrders()).isEqualTo(4);
        assertThat(response.activeOrders()).isEqualTo(3);
        assertThat(response.inLeaseCount()).isEqualTo(1);
        assertThat(response.pendingReturns()).isEqualTo(1);
        assertThat(response.totalGmv()).isEqualByComparingTo("600.00");
        assertThat(response.ordersByStatus().get(OrderStatus.RETURN_REQUESTED)).isEqualTo(1);
    }

    @Test
    void shouldCalculateVendorMetrics() {
        VendorMetricsResponse response = orderAnalyticsService.getVendorMetrics(vendorId);
        assertThat(response.totalOrders()).isEqualTo(4);
        assertThat(response.activeOrders()).isEqualTo(3);
        assertThat(response.pendingReturns()).isEqualTo(1);
        assertThat(response.totalGmv()).isEqualByComparingTo("600.00");
    }

    private RentalOrder createOrder(OrderStatus targetStatus, BigDecimal totalAmount) {
        RentalOrder order = RentalOrder.create(
                userId,
                vendorId,
                "STANDARD",
                totalAmount.divide(BigDecimal.valueOf(2)),
                totalAmount.divide(BigDecimal.valueOf(2)),
                null,
                totalAmount,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusMonths(12)
        );
        RentalOrderItem item = RentalOrderItem.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "测试商品",
                "SKU-1",
                null,
                1,
                totalAmount.divide(BigDecimal.valueOf(2)),
                totalAmount.divide(BigDecimal.valueOf(2)),
                null
        );
        order.addItem(item);
        if (targetStatus == OrderStatus.PENDING_PAYMENT) {
            return order;
        }
        order.markPaid();
        if (targetStatus == OrderStatus.AWAITING_SHIPMENT) {
            return order;
        }
        order.ship("SF", "TRACK");
        if (targetStatus == OrderStatus.IN_LEASE) {
            return order;
        }
        if (targetStatus == OrderStatus.RETURN_REQUESTED) {
            order.requestReturn();
        }
        return order;
    }
}
