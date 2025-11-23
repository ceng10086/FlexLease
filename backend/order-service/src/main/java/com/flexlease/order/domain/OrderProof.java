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
@Table(name = "order_proof", schema = "order")
public class OrderProof {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "proof_type", nullable = false, length = 30)
    private OrderProofType proofType;

    @Column(name = "description")
    private String description;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 255)
    private String fileUrl;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", length = 20)
    private OrderActorRole actorRole;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    protected OrderProof() {
        // JPA
    }

    private OrderProof(OrderProofType proofType,
                       String description,
                       String fileName,
                       String fileUrl,
                       String contentType,
                       long fileSize,
                       UUID uploadedBy,
                       OrderActorRole actorRole) {
        this.id = UUID.randomUUID();
        this.proofType = proofType;
        this.description = description;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.actorRole = actorRole;
        this.uploadedAt = OffsetDateTime.now();
    }

    public static OrderProof create(OrderProofType proofType,
                                    String description,
                                    String fileName,
                                    String fileUrl,
                                    String contentType,
                                    long fileSize,
                                    UUID uploadedBy,
                                    OrderActorRole actorRole) {
        return new OrderProof(proofType, description, fileName, fileUrl, contentType, fileSize, uploadedBy, actorRole);
    }

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = OffsetDateTime.now();
        }
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

    public OrderProofType getProofType() {
        return proofType;
    }

    public String getDescription() {
        return description;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public OrderActorRole getActorRole() {
        return actorRole;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }
}
