package com.flexlease.product.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "product", schema = "product")
public class Product {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "category_code", nullable = false, length = 100)
    private String categoryCode;

    @Column(name = "description")
    private String description;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProductStatus status;

    @Column(name = "review_remark")
    private String reviewRemark;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RentalPlan> rentalPlans = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, createdAt ASC")
    private Set<MediaAsset> mediaAssets = new LinkedHashSet<>();

    protected Product() {
        // JPA 需要无参构造
    }

    private Product(UUID vendorId,
                    String name,
                    String categoryCode,
                    String description,
                    String coverImageUrl) {
        this.id = UUID.randomUUID();
        this.vendorId = vendorId;
        this.name = name;
        this.categoryCode = categoryCode;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.status = ProductStatus.DRAFT;
    }

    public static Product create(UUID vendorId,
                                 String name,
                                 String categoryCode,
                                 String description,
                                 String coverImageUrl) {
        return new Product(vendorId, name, categoryCode, description, coverImageUrl);
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

    public UUID getVendorId() {
        return vendorId;
    }

    public String getName() {
        return name;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public String getReviewRemark() {
        return reviewRemark;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Set<RentalPlan> getRentalPlans() {
        return rentalPlans;
    }

    public Set<MediaAsset> getMediaAssets() {
        return mediaAssets;
    }

    public void updateBasicInfo(String name,
                                String categoryCode,
                                String description,
                                String coverImageUrl) {
        this.name = name;
        this.categoryCode = categoryCode;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
    }

    public void addMediaAsset(MediaAsset asset) {
        mediaAssets.add(asset);
        asset.setProduct(this);
    }

    public void removeMediaAsset(MediaAsset asset) {
        mediaAssets.remove(asset);
        asset.setProduct(null);
    }

    public void submitForReview() {
        this.status = ProductStatus.PENDING_REVIEW;
        this.submittedAt = OffsetDateTime.now();
        this.reviewedAt = null;
        this.reviewedBy = null;
    }

    public void markApproved(UUID reviewerId, String remark) {
        this.status = ProductStatus.ACTIVE;
        this.reviewedAt = OffsetDateTime.now();
        this.reviewRemark = remark;
        this.reviewedBy = reviewerId;
    }

    public void markRejected(UUID reviewerId, String remark) {
        this.status = ProductStatus.REJECTED;
        this.reviewedAt = OffsetDateTime.now();
        this.reviewRemark = remark;
        this.reviewedBy = reviewerId;
    }

    public void markInactive() {
        this.status = ProductStatus.INACTIVE;
    }

    public void resetReviewRemark() {
        this.reviewRemark = null;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }
}
