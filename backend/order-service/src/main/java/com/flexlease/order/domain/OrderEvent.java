package com.flexlease.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_event", schema = "order")
public class OrderEvent {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private OrderEventType eventType;

    @Column(name = "description")
    private String description;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected OrderEvent() {
        // JPA
    }

    private OrderEvent(OrderEventType eventType, String description, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.eventType = eventType;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = OffsetDateTime.now();
    }

    public static OrderEvent record(OrderEventType type, String description, UUID createdBy) {
        return new OrderEvent(type, description, createdBy);
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public RentalOrder getOrder() {
        return order;
    }

    public void setOrder(RentalOrder order) {
        this.order = order;
    }

    public OrderEventType getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
