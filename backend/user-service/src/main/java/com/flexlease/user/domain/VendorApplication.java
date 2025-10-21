package com.flexlease.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendor_application", schema = "users")
public class VendorApplication {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "unified_social_code", nullable = false, unique = true)
    private String unifiedSocialCode;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "province")
    private String province;

    @Column(name = "city")
    private String city;

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VendorApplicationStatus status;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "review_remark")
    private String reviewRemark;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected VendorApplication() {
        // JPA
    }

    private VendorApplication(UUID ownerUserId,
                              String companyName,
                              String unifiedSocialCode,
                              String contactName,
                              String contactPhone,
                              String contactEmail,
                              String province,
                              String city,
                              String address) {
        this.id = UUID.randomUUID();
        this.ownerUserId = ownerUserId;
        this.companyName = companyName;
        this.unifiedSocialCode = unifiedSocialCode;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.province = province;
        this.city = city;
        this.address = address;
        this.status = VendorApplicationStatus.SUBMITTED;
        this.submittedAt = OffsetDateTime.now();
    }

    public static VendorApplication submit(UUID ownerUserId,
                                            String companyName,
                                            String unifiedSocialCode,
                                            String contactName,
                                            String contactPhone,
                                            String contactEmail,
                                            String province,
                                            String city,
                                            String address) {
        return new VendorApplication(ownerUserId, companyName, unifiedSocialCode, contactName, contactPhone, contactEmail, province, city, address);
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

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getUnifiedSocialCode() {
        return unifiedSocialCode;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public VendorApplicationStatus getStatus() {
        return status;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewRemark() {
        return reviewRemark;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void approve(UUID reviewerId, String remark) {
        this.status = VendorApplicationStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewRemark = remark;
        this.reviewedAt = OffsetDateTime.now();
    }

    public void reject(UUID reviewerId, String remark) {
        this.status = VendorApplicationStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewRemark = remark;
        this.reviewedAt = OffsetDateTime.now();
    }
}
