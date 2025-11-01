package com.flexlease.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rental_contract", schema = "order")
public class OrderContract {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private RentalOrder order;

    @Column(name = "contract_number", nullable = false, length = 50)
    private String contractNumber;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderContractStatus status;

    @Column(name = "signature", length = 255)
    private String signature;

    @Column(name = "signed_by")
    private UUID signedBy;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected OrderContract() {
        // JPA
    }

    private OrderContract(RentalOrder order,
                          String contractNumber,
                          String content) {
        this.id = UUID.randomUUID();
        this.order = order;
        this.contractNumber = contractNumber;
        this.content = content;
        this.status = OrderContractStatus.DRAFT;
        this.generatedAt = OffsetDateTime.now();
    }

    public static OrderContract draft(RentalOrder order, String contractNumber, String content) {
        return new OrderContract(order, contractNumber, content);
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public RentalOrder getOrder() {
        return order;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public String getContent() {
        return content;
    }

    public OrderContractStatus getStatus() {
        return status;
    }

    public String getSignature() {
        return signature;
    }

    public UUID getSignedBy() {
        return signedBy;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public OffsetDateTime getSignedAt() {
        return signedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isSigned() {
        return status == OrderContractStatus.SIGNED;
    }

    public void sign(UUID userId, String signatureContent) {
        if (isSigned()) {
            return;
        }
        this.status = OrderContractStatus.SIGNED;
        this.signedAt = OffsetDateTime.now();
        this.signedBy = userId;
        this.signature = signatureContent;
    }

    public void refreshContent(String content) {
        this.content = content;
    }
}
