package com.flexlease.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexlease.common.messaging.MessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 相关配置。
 * <p>
 * 订单服务会向 {@code order.events} 交换机投递订单事件；通知服务绑定自身队列用于消费并推送站内信。
 */
@Configuration
@EnableRabbit
@ConditionalOnProperty(prefix = "flexlease.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessagingConfig {

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(MessagingConstants.ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderEventsQueue() {
        return new Queue(MessagingConstants.ORDER_EVENTS_NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding orderEventsBinding(Queue orderEventsQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderEventsQueue)
                .to(orderEventsExchange)
                .with(MessagingConstants.ORDER_EVENTS_ROUTING_KEY_PREFIX + "#");
    }
}
