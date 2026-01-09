package com.flexlease.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexlease.common.messaging.MessagingConstants;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 发布端配置（订单事件）。
 * <p>
 * 订单服务负责把关键状态变更发布到 {@code order.events} 交换机，供 notification-service 等订阅者消费。
 */
@Configuration
@ConditionalOnProperty(prefix = "flexlease.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessagingConfig {

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(MessagingConstants.ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
