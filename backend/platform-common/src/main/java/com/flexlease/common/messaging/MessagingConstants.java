package com.flexlease.common.messaging;

/**
 * Shared messaging constants for cross-service communication.
 */
public final class MessagingConstants {

    private MessagingConstants() {
        // utility
    }

    /**
     * Topic exchange for order domain events.
     */
    public static final String ORDER_EVENTS_EXCHANGE = "order.events";

    /**
     * Default routing key prefix for order domain events.
     */
    public static final String ORDER_EVENTS_ROUTING_KEY_PREFIX = "order.";

    /**
     * Queue used by notification-service to subscribe order events.
     */
    public static final String ORDER_EVENTS_NOTIFICATION_QUEUE = "order.events.notification";
}
