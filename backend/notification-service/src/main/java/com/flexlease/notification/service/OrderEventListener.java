package com.flexlease.notification.service;

import com.flexlease.common.audit.BusinessReplayLogWriter;
import com.flexlease.common.messaging.MessagingConstants;
import com.flexlease.common.messaging.OrderEventMessage;
import com.flexlease.common.notification.NotificationSendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 订单事件监听器：订阅 RabbitMQ 的订单事件队列，并转化为站内信提醒。
 * <p>
 * 当前仅处理 {@code ORDER_CREATED}，用于提醒厂商“新订单待处理”；其他事件可按需扩展。
 */
@Component
@ConditionalOnProperty(prefix = "flexlease.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService;
    private final BusinessReplayLogWriter replayLogWriter;

    public OrderEventListener(NotificationService notificationService,
                              BusinessReplayLogWriter replayLogWriter) {
        this.notificationService = notificationService;
        this.replayLogWriter = replayLogWriter;
    }

    @RabbitListener(queues = MessagingConstants.ORDER_EVENTS_NOTIFICATION_QUEUE)
    public void onOrderEvent(OrderEventMessage message) {
        replayLogWriter.writeIncoming(
                MessagingConstants.ORDER_EVENTS_EXCHANGE,
                null,
                message.eventType(),
                "RentalOrder",
                message.orderId(),
                message,
                message.occurredAt()
        );
        if ("ORDER_CREATED".equalsIgnoreCase(message.eventType())) {
            dispatchNewOrderNotification(message);
        }
    }

    private void dispatchNewOrderNotification(OrderEventMessage message) {
        if (message.vendorId() == null) {
            LOG.warn("Skip order created event without vendor id: {}", message);
            return;
        }
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                message.vendorId().toString(),
                "新订单待处理",
                "订单 %s 已创建，请尽快确认并安排履约。".formatted(message.orderNo()),
                java.util.Map.of(
                        "orderId", message.orderId().toString(),
                        "eventType", message.eventType()
                )
        );
        notificationService.sendNotification(request);
    }
}
