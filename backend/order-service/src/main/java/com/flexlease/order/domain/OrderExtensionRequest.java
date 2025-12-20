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
@Table(name = "order_extension_request", schema = "order")
public class OrderExtensionRequest {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExtensionRequestStatus status;

    @Column(name = "additional_months", nullable = false)
    private int additionalMonths;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "decision_by")
    private UUID decisionBy;

    @Column(name = "decision_at")
    private OffsetDateTime decisionAt;

    @Column(name = "remark")
    private String remark;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected OrderExtensionRequest() {
        // JPA 需要无参构造
    }

    private OrderExtensionRequest(int additionalMonths, UUID requestedBy, String remark) {
        this.id = UUID.randomUUID();
        this.status = ExtensionRequestStatus.PENDING;
        this.additionalMonths = additionalMonths;
        this.requestedBy = requestedBy;
        this.requestedAt = OffsetDateTime.now();
        this.remark = remark;
    }

    public static OrderExtensionRequest create(int additionalMonths, UUID requestedBy, String remark) {
        return new OrderExtensionRequest(additionalMonths, requestedBy, remark);
    }

    @PrePersist
    void onCreate() {
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
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

    public ExtensionRequestStatus getStatus() {
        return status;
    }

    public int getAdditionalMonths() {
        return additionalMonths;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public UUID getDecisionBy() {
        return decisionBy;
    }

    public OffsetDateTime getDecisionAt() {
        return decisionAt;
    }

    public String getRemark() {
        return remark;
    }

    public void approve(UUID decisionBy, String remark) {
        if (status != ExtensionRequestStatus.PENDING) {
            throw new IllegalStateException("续租申请状态已处理");
        }
        this.status = ExtensionRequestStatus.APPROVED;
        this.decisionBy = decisionBy;
        this.decisionAt = OffsetDateTime.now();
        this.remark = remark;
    }

    public void reject(UUID decisionBy, String remark) {
        if (status != ExtensionRequestStatus.PENDING) {
            throw new IllegalStateException("续租申请状态已处理");
        }
        this.status = ExtensionRequestStatus.REJECTED;
        this.decisionBy = decisionBy;
        this.decisionAt = OffsetDateTime.now();
        this.remark = remark;
    }
}
