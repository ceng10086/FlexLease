package com.flexlease.product.domain;

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
@Table(name = "product_inquiry", schema = "product")
public class ProductInquiry {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "requester_id")
    private UUID requesterId;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_method", length = 120)
    private String contactMethod;

    @Column(name = "message", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductInquiryStatus status;

    @Column(name = "reply")
    private String reply;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ProductInquiry() {
        // JPA
    }

    private ProductInquiry(Product product,
                           UUID vendorId,
                           UUID requesterId,
                           String contactName,
                           String contactMethod,
                           String message,
                           OffsetDateTime expiresAt) {
        this.id = UUID.randomUUID();
        this.product = product;
        this.vendorId = vendorId;
        this.requesterId = requesterId;
        this.contactName = contactName;
        this.contactMethod = contactMethod;
        this.message = message;
        this.expiresAt = expiresAt;
        this.status = ProductInquiryStatus.OPEN;
    }

    public static ProductInquiry create(Product product,
                                        UUID vendorId,
                                        UUID requesterId,
                                        String contactName,
                                        String contactMethod,
                                        String message,
                                        OffsetDateTime expiresAt) {
        return new ProductInquiry(product, vendorId, requesterId, contactName, contactMethod, message, expiresAt);
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactMethod() {
        return contactMethod;
    }

    public String getMessage() {
        return message;
    }

    public ProductInquiryStatus getStatus() {
        return status;
    }

    public String getReply() {
        return reply;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void reply(String reply) {
        this.reply = reply;
        this.status = ProductInquiryStatus.RESPONDED;
        this.respondedAt = OffsetDateTime.now();
    }

    public void expire() {
        if (this.status == ProductInquiryStatus.OPEN) {
            this.status = ProductInquiryStatus.EXPIRED;
        }
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(OffsetDateTime.now());
    }

    public void refreshExpirationState() {
        if (isExpired()) {
            expire();
        }
    }
}
