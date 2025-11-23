package com.flexlease.order.service;

import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderEvent;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.RentalOrder;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderTimelineService {

    private final OrderEventPublisher orderEventPublisher;

    public OrderTimelineService(OrderEventPublisher orderEventPublisher) {
        this.orderEventPublisher = orderEventPublisher;
    }

    public void append(RentalOrder order,
                       OrderEventType eventType,
                       String description,
                       UUID actorId,
                       Map<String, Object> attributes,
                       OrderActorRole actorRole) {
        order.addEvent(OrderEvent.record(eventType, description, actorId, actorRole));
        orderEventPublisher.publish(order, eventType, description, actorId, attributes);
    }
}
