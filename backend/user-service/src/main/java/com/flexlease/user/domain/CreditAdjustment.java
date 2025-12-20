package com.flexlease.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_adjustment", schema = "users")
public class CreditAdjustment {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "delta", nullable = false)
    private int delta;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "operator_id")
    private UUID operatorId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected CreditAdjustment() {
        // JPA 需要无参构造
    }

    private CreditAdjustment(UUID id, UUID userId, int delta, String reason, UUID operatorId) {
        this.id = id;
        this.userId = userId;
        this.delta = delta;
        this.reason = reason;
        this.operatorId = operatorId;
    }

    public static CreditAdjustment create(UUID userId, int delta, String reason, UUID operatorId) {
        return new CreditAdjustment(UUID.randomUUID(), userId, delta, reason, operatorId);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getDelta() {
        return delta;
    }

    public String getReason() {
        return reason;
    }

    public UUID getOperatorId() {
        return operatorId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
