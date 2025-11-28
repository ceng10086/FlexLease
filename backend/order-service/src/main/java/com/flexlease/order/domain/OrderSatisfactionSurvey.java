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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_satisfaction_survey", schema = "order")
public class OrderSatisfactionSurvey {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id")
    private OrderDispute dispute;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false, length = 20)
    private OrderActorRole targetRole;

    @Column(name = "target_ref", nullable = false)
    private UUID targetRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderSurveyStatus status;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "comment")
    private String comment;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "available_at", nullable = false)
    private OffsetDateTime availableAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected OrderSatisfactionSurvey() {
        // JPA
    }

    private OrderSatisfactionSurvey(OrderActorRole targetRole, UUID targetRef, OffsetDateTime availableAt) {
        this.id = UUID.randomUUID();
        this.targetRole = targetRole;
        this.targetRef = targetRef;
        this.availableAt = availableAt;
        this.status = OrderSurveyStatus.PENDING;
        this.requestedAt = OffsetDateTime.now();
    }

    public static OrderSatisfactionSurvey create(OrderActorRole targetRole,
                                                 UUID targetRef,
                                                 OffsetDateTime availableAt) {
        return new OrderSatisfactionSurvey(targetRole, targetRef, availableAt);
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
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

    public OrderDispute getDispute() {
        return dispute;
    }

    public void setDispute(OrderDispute dispute) {
        this.dispute = dispute;
    }

    public OrderActorRole getTargetRole() {
        return targetRole;
    }

    public UUID getTargetRef() {
        return targetRef;
    }

    public OrderSurveyStatus getStatus() {
        return status;
    }

    public Integer getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public OffsetDateTime getAvailableAt() {
        return availableAt;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public boolean isReminderSent() {
        return reminderSent;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markOpen() {
        if (status == OrderSurveyStatus.PENDING) {
            this.status = OrderSurveyStatus.OPEN;
        }
    }

    public void markCompleted(int rating, String comment) {
        this.rating = rating;
        this.comment = comment;
        this.submittedAt = OffsetDateTime.now();
        this.status = OrderSurveyStatus.COMPLETED;
    }

    public void markReminderSent() {
        this.reminderSent = true;
    }
}
