package com.flexlease.order.service;

import com.flexlease.common.notification.NotificationChannel;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.order.client.InventoryReservationClient;
import com.flexlease.order.client.InventoryReservationClient.InventoryCommand;
import com.flexlease.order.client.NotificationClient;
import com.flexlease.order.config.OrderMaintenanceProperties;
import com.flexlease.order.domain.OrderEvent;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.repository.RentalOrderRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderMaintenanceScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(OrderMaintenanceScheduler.class);

    private final RentalOrderRepository rentalOrderRepository;
    private final InventoryReservationClient inventoryReservationClient;
    private final NotificationClient notificationClient;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderMaintenanceProperties properties;
    private final TransactionTemplate transactionTemplate;

    public OrderMaintenanceScheduler(RentalOrderRepository rentalOrderRepository,
                                     InventoryReservationClient inventoryReservationClient,
                                     NotificationClient notificationClient,
                                     OrderEventPublisher orderEventPublisher,
                                     OrderMaintenanceProperties properties,
                                     TransactionTemplate transactionTemplate) {
        this.rentalOrderRepository = rentalOrderRepository;
        this.inventoryReservationClient = inventoryReservationClient;
        this.notificationClient = notificationClient;
        this.orderEventPublisher = orderEventPublisher;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelayString = "${flexlease.order.maintenance.scan-interval-ms:60000}")
    public void cancelExpiredPendingOrders() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(properties.getPendingPaymentExpireMinutes());
        List<RentalOrder> expiredOrders = rentalOrderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, cutoff);
        if (expiredOrders.isEmpty()) {
            return;
        }
        LOG.debug("Found {} pending payment orders exceeding {} minutes", expiredOrders.size(), properties.getPendingPaymentExpireMinutes());
        expiredOrders.stream()
                .map(RentalOrder::getId)
                .forEach(this::cancelExpiredOrderSafely);
    }

    private void cancelExpiredOrderSafely(UUID orderId) {
        transactionTemplate.executeWithoutResult(status -> {
            RentalOrder order = rentalOrderRepository.findByIdWithDetails(orderId).orElse(null);
            if (order == null) {
                return;
            }
            try {
                handleExpiration(order);
            } catch (RuntimeException ex) {
                status.setRollbackOnly();
                LOG.warn("Failed to auto cancel order {}: {}", order.getOrderNo(), ex.getMessage());
            }
        });
    }

    private void handleExpiration(RentalOrder order) {
        List<InventoryCommand> commands = order.getItems().stream()
                .filter(item -> item.getSkuId() != null)
                .map(item -> new InventoryCommand(item.getSkuId(), item.getQuantity()))
                .toList();
        try {
            order.cancel();
        } catch (IllegalStateException ex) {
            LOG.debug("Skip auto-cancel for order {} due to state change: {}", order.getId(), ex.getMessage());
            return;
        }
        OrderEvent event = OrderEvent.record(OrderEventType.ORDER_CANCELLED, "系统自动取消订单：支付超时", null);
        order.addEvent(event);
        orderEventPublisher.publish(order, OrderEventType.ORDER_CANCELLED, event.getDescription(), null, Map.of("reason", "PAYMENT_TIMEOUT"));
        rentalOrderRepository.save(order);

        if (!commands.isEmpty()) {
            try {
                inventoryReservationClient.release(order.getId(), commands);
            } catch (RuntimeException ex) {
                LOG.warn("Failed to release inventory for expired order {}: {}", order.getId(), ex.getMessage());
            }
        }

        sendAutoCancelNotifications(order);
        LOG.info("Automatically cancelled pending payment order {}", order.getOrderNo());
    }

    private void sendAutoCancelNotifications(RentalOrder order) {
        Map<String, Object> payload = Map.of("orderNo", order.getOrderNo());
        NotificationSendRequest userNotification = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                order.getUserId().toString(),
                "订单已自动取消",
                "订单 %s 因支付超时已被自动取消。".formatted(order.getOrderNo()),
                payload
        );
        try {
            notificationClient.send(userNotification);
        } catch (RuntimeException ex) {
            LOG.warn("Failed to notify user about auto cancellation for order {}: {}",
                    order.getOrderNo(), ex.getMessage());
        }

        NotificationSendRequest vendorNotification = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                order.getVendorId().toString(),
                "订单已自动取消",
                "订单 %s 因支付超时被系统自动取消，请确认库存记录。".formatted(order.getOrderNo()),
                payload
        );
        try {
            notificationClient.send(vendorNotification);
        } catch (RuntimeException ex) {
            LOG.warn("Failed to notify vendor about auto cancellation for order {}: {}",
                    order.getOrderNo(), ex.getMessage());
        }
    }
}
