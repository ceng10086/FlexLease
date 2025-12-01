package com.flexlease.order.service;

import com.flexlease.common.messaging.MessagingConstants;
import com.flexlease.common.messaging.OrderEventMessage;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.RentalOrder;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class OrderEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final boolean messagingEnabled;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate,
                               @Value("${flexlease.messaging.enabled:true}") boolean messagingEnabled) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingEnabled = messagingEnabled;
    }

    public void publish(RentalOrder order,
                        OrderEventType eventType,
                        String description,
                        UUID actorId,
                        Map<String, Object> attributes) {
        if (!messagingEnabled) {
            return;
        }
        OrderEventMessage payload = new OrderEventMessage(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                order.getVendorId(),
                order.getStatus().name(),
                eventType.name(),
                OffsetDateTime.now(),
                actorId,
                description,
                attributes == null || attributes.isEmpty() ? Map.of() : attributes
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchEvent(payload, eventType, order.getOrderNo());
                }
            });
        } else {
            dispatchEvent(payload, eventType, order.getOrderNo());
        }
    }

    private void dispatchEvent(OrderEventMessage payload, OrderEventType eventType, String orderNo) {
        String routingKey = MessagingConstants.ORDER_EVENTS_ROUTING_KEY_PREFIX + eventType.name().toLowerCase();
        try {
            rabbitTemplate.convertAndSend(MessagingConstants.ORDER_EVENTS_EXCHANGE, routingKey, payload);
        } catch (AmqpException ex) {
            LOG.warn("Failed to publish order event {} for order {}: {}", eventType, orderNo, ex.getMessage());
        }
    }
}
