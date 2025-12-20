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
@Table(name = "order_return_request", schema = "order")
public class OrderReturnRequest {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReturnRequestStatus status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "logistics_company", length = 100)
    private String logisticsCompany;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

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

    protected OrderReturnRequest() {
        // JPA 需要无参构造
    }

    private OrderReturnRequest(String reason,
                               String logisticsCompany,
                               String trackingNumber,
                               UUID requestedBy) {
        this.id = UUID.randomUUID();
        this.status = ReturnRequestStatus.PENDING;
        this.reason = reason;
        this.logisticsCompany = logisticsCompany;
        this.trackingNumber = trackingNumber;
        this.requestedBy = requestedBy;
        this.requestedAt = OffsetDateTime.now();
    }

    public static OrderReturnRequest create(String reason,
                                            String logisticsCompany,
                                            String trackingNumber,
                                            UUID requestedBy) {
        return new OrderReturnRequest(reason, logisticsCompany, trackingNumber, requestedBy);
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

    public ReturnRequestStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getLogisticsCompany() {
        return logisticsCompany;
    }

    public String getTrackingNumber() {
        return trackingNumber;
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
        if (status != ReturnRequestStatus.PENDING) {
            throw new IllegalStateException("退租申请状态已处理");
        }
        this.status = ReturnRequestStatus.APPROVED;
        this.decisionBy = decisionBy;
        this.decisionAt = OffsetDateTime.now();
        this.remark = remark;
    }

    public void reject(UUID decisionBy, String remark) {
        if (status != ReturnRequestStatus.PENDING) {
            throw new IllegalStateException("退租申请状态已处理");
        }
        this.status = ReturnRequestStatus.REJECTED;
        this.decisionBy = decisionBy;
        this.decisionAt = OffsetDateTime.now();
        this.remark = remark;
    }
}
