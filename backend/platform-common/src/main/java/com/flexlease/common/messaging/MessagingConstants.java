package com.flexlease.common.messaging;

/**
 * 跨服务通信的消息常量。
 */
public final class MessagingConstants {

    private MessagingConstants() {
        // 工具类：禁止实例化
    }

    /**
     * 订单领域事件 Topic Exchange 名称。
     */
    public static final String ORDER_EVENTS_EXCHANGE = "order.events";

    /**
     * 订单领域事件默认路由键前缀。
     */
    public static final String ORDER_EVENTS_ROUTING_KEY_PREFIX = "order.";

    /**
     * notification-service 订阅订单事件使用的队列名称。
     */
    public static final String ORDER_EVENTS_NOTIFICATION_QUEUE = "order.events.notification";
}
