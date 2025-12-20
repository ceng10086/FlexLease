package com.flexlease.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "refund_transaction", schema = "payment")
public class RefundTransaction {

    private static final DateTimeFormatter NUMBER_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.CHINA);

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private PaymentTransaction transaction;

    @Column(name = "refund_no", nullable = false, unique = true, length = 60)
    private String refundNo;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "reason", length = 200)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status;

    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected RefundTransaction() {
        // JPA 需要无参构造
    }

    private RefundTransaction(BigDecimal amount, String reason) {
        this.id = UUID.randomUUID();
        this.refundNo = generateRefundNo();
        this.amount = amount;
        this.reason = reason;
        this.status = RefundStatus.PROCESSING;
    }

    public static RefundTransaction create(BigDecimal amount, String reason) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("退款金额必须大于 0");
        }
        return new RefundTransaction(amount, reason);
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public PaymentTransaction getTransaction() {
        return transaction;
    }

    public String getRefundNo() {
        return refundNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public OffsetDateTime getRefundedAt() {
        return refundedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markSucceeded() {
        if (status == RefundStatus.SUCCEEDED) {
            return;
        }
        this.status = RefundStatus.SUCCEEDED;
        this.refundedAt = OffsetDateTime.now();
    }

    public void markFailed() {
        this.status = RefundStatus.FAILED;
        this.refundedAt = null;
    }

    void setTransaction(PaymentTransaction transaction) {
        this.transaction = transaction;
    }

    private String generateRefundNo() {
        String timestamp = OffsetDateTime.now().format(NUMBER_FORMATTER);
        String tail = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        return "R" + timestamp + tail;
    }
}
