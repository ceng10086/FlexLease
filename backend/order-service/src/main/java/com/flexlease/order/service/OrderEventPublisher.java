package com.flexlease.order.service;

import com.flexlease.common.audit.BusinessReplayLogWriter;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 订单事件发布器：将订单状态变更发布到消息队列（RabbitMQ）。
 * <p>
 * 为避免“事务未提交但消息已发出”的不一致问题，默认在事务提交后（afterCommit）再发送消息。
 * 同时会写入业务回放日志（audit.business_replay_log）用于排查与复盘。
 */
@Component
public class OrderEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final BusinessReplayLogWriter replayLogWriter;
    private final boolean messagingEnabled;

    public OrderEventPublisher(ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
                               BusinessReplayLogWriter replayLogWriter,
                               @Value("${flexlease.messaging.enabled:true}") boolean messagingEnabled) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
        this.replayLogWriter = replayLogWriter;
        this.messagingEnabled = messagingEnabled;
    }

    public void publish(RentalOrder order,
                        OrderEventType eventType,
                        String description,
                        UUID actorId,
                        Map<String, Object> attributes) {
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
        replayLogWriter.writeOutgoing(
                MessagingConstants.ORDER_EVENTS_EXCHANGE,
                routingKey,
                eventType.name(),
                "RentalOrder",
                payload.orderId(),
                payload,
                payload.occurredAt()
        );
        if (!messagingEnabled) {
            return;
        }
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            LOG.debug("Skip publishing order event {} because RabbitTemplate is not available.", eventType);
            return;
        }
        try {
            rabbitTemplate.convertAndSend(MessagingConstants.ORDER_EVENTS_EXCHANGE, routingKey, payload);
        } catch (AmqpException ex) {
            LOG.warn("Failed to publish order event {} for order {}: {}", eventType, orderNo, ex.getMessage());
        }
    }
}
