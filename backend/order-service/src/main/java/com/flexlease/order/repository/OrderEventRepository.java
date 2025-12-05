package com.flexlease.order.repository;

import com.flexlease.order.domain.OrderEvent;
import com.flexlease.order.domain.OrderEventType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderEventRepository extends JpaRepository<OrderEvent, UUID> {

    @Query("""
            select e.order.id as orderId,
                   e.order.createdAt as orderCreatedAt,
                   e.createdAt as eventCreatedAt
            from OrderEvent e
            where e.order.vendorId = :vendorId and e.eventType = :eventType
            """)
    List<EventTimestamp> findEventTimestampsByVendorAndType(@Param("vendorId") UUID vendorId,
                                                            @Param("eventType") OrderEventType eventType);

    interface EventTimestamp {
        UUID getOrderId();

        OffsetDateTime getOrderCreatedAt();

        OffsetDateTime getEventCreatedAt();
    }
}
