package com.flexlease.order.service;

import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderEvent;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.RentalOrder;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 订单时间线服务：统一追加订单事件，并同步发布到事件总线。
 * <p>
 * 业务服务在变更订单状态、取证上传、纠纷处理等节点调用该服务，避免各处重复写事件/发消息逻辑。
 */
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
