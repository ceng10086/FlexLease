package com.flexlease.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_split", schema = "payment")
public class PaymentSplit {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private PaymentTransaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 30)
    private PaymentSplitType splitType;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "beneficiary", nullable = false, length = 100)
    private String beneficiary;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected PaymentSplit() {
        // JPA 需要无参构造
    }

    private PaymentSplit(PaymentSplitType splitType, BigDecimal amount, String beneficiary) {
        this.id = UUID.randomUUID();
        this.splitType = splitType;
        this.amount = amount;
        this.beneficiary = beneficiary;
    }

    public static PaymentSplit create(PaymentSplitType splitType, BigDecimal amount, String beneficiary) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("分账金额不能为空且不能为负");
        }
        if (beneficiary == null || beneficiary.isBlank()) {
            throw new IllegalArgumentException("分账收益人不能为空");
        }
        return new PaymentSplit(splitType, amount, beneficiary);
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public PaymentTransaction getTransaction() {
        return transaction;
    }

    public PaymentSplitType getSplitType() {
        return splitType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getBeneficiary() {
        return beneficiary;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    void setTransaction(PaymentTransaction transaction) {
        this.transaction = transaction;
    }
}
