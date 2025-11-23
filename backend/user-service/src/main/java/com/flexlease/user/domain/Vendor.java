package com.flexlease.user.domain;

import com.flexlease.common.user.CreditTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "vendor", schema = "users")
public class Vendor {

    private static final String DEFAULT_INDUSTRY = "GENERAL";
    private static final BigDecimal DEFAULT_BASE_RATE = new BigDecimal("0.0800");
    private static final int DEFAULT_SLA_SCORE = 80;
    private static final BigDecimal MIN_RATE = new BigDecimal("0.0300");
    private static final BigDecimal MAX_RATE = new BigDecimal("0.2000");

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "contact_phone", nullable = false, length = 50)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "address", length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private VendorStatus status;

    @Column(name = "industry_category", nullable = false, length = 100)
    private String industryCategory;

    @Column(name = "commission_base_rate", nullable = false)
    private BigDecimal commissionBaseRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_credit_tier", nullable = false, length = 30)
    private CreditTier commissionCreditTier;

    @Column(name = "commission_sla_score", nullable = false)
    private Integer commissionSlaScore;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Vendor() {
        // JPA
    }

    private Vendor(UUID id,
                   UUID ownerUserId,
                   String companyName,
                   String contactName,
                   String contactPhone,
                   String contactEmail,
                   String province,
                   String city,
                   String address,
                   VendorStatus status,
                   String industryCategory,
                   BigDecimal commissionBaseRate,
                   CreditTier commissionCreditTier,
                   Integer commissionSlaScore) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.companyName = companyName;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.province = province;
        this.city = city;
        this.address = address;
        this.status = status;
        this.industryCategory = normalizeIndustry(industryCategory);
        this.commissionBaseRate = normalizeBaseRate(commissionBaseRate);
        this.commissionCreditTier = commissionCreditTier == null ? CreditTier.STANDARD : commissionCreditTier;
        this.commissionSlaScore = normalizeSlaScore(commissionSlaScore);
    }

    public static Vendor create(UUID ownerUserId,
                                String companyName,
                                String contactName,
                                String contactPhone,
                                String contactEmail,
                                String province,
                                String city,
                                String address) {
        return new Vendor(UUID.randomUUID(),
                ownerUserId,
                companyName,
                contactName,
                contactPhone,
                contactEmail,
                province,
                city,
                address,
                VendorStatus.ACTIVE,
                DEFAULT_INDUSTRY,
                DEFAULT_BASE_RATE,
                CreditTier.STANDARD,
                DEFAULT_SLA_SCORE);
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

    public VendorStatus getStatus() {
        return status;
    }

    public String getIndustryCategory() {
        return industryCategory;
    }

    public BigDecimal getCommissionBaseRate() {
        return commissionBaseRate;
    }

    public CreditTier getCommissionCreditTier() {
        return commissionCreditTier;
    }

    public Integer getCommissionSlaScore() {
        return commissionSlaScore;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateContactInfo(String contactName,
                                  String contactPhone,
                                  String contactEmail,
                                  String province,
                                  String city,
                                  String address) {
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.province = province;
        this.city = city;
        this.address = address;
    }

    public void updateStatus(VendorStatus status) {
        this.status = status;
    }

    public void updateCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void updateCommissionProfile(String industryCategory,
                                        BigDecimal baseRate,
                                        CreditTier creditTier,
                                        Integer slaScore) {
        this.industryCategory = normalizeIndustry(industryCategory);
        this.commissionBaseRate = normalizeBaseRate(baseRate);
        this.commissionCreditTier = creditTier == null ? CreditTier.STANDARD : creditTier;
        this.commissionSlaScore = normalizeSlaScore(slaScore);
    }

    public BigDecimal calculateCommissionRate() {
        BigDecimal rate = commissionBaseRate != null ? commissionBaseRate : DEFAULT_BASE_RATE;
        if (commissionCreditTier != null) {
            rate = switch (commissionCreditTier) {
                case EXCELLENT -> rate.subtract(new BigDecimal("0.0200"));
                case STANDARD -> rate;
                case WARNING -> rate.add(new BigDecimal("0.0200"));
                case RESTRICTED -> rate.add(new BigDecimal("0.0300"));
            };
        }
        if (commissionSlaScore != null) {
            if (commissionSlaScore >= 90) {
                rate = rate.subtract(new BigDecimal("0.0050"));
            } else if (commissionSlaScore < 70) {
                rate = rate.add(new BigDecimal("0.0100"));
            }
        }
        if (rate.compareTo(MIN_RATE) < 0) {
            rate = MIN_RATE;
        }
        if (rate.compareTo(MAX_RATE) > 0) {
            rate = MAX_RATE;
        }
        return rate.setScale(4, RoundingMode.HALF_UP);
    }

    private String normalizeIndustry(String industry) {
        if (industry == null || industry.isBlank()) {
            return DEFAULT_INDUSTRY;
        }
        return industry.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizeBaseRate(BigDecimal baseRate) {
        if (baseRate == null) {
            return DEFAULT_BASE_RATE;
        }
        BigDecimal normalized = baseRate.setScale(4, RoundingMode.HALF_UP);
        if (normalized.compareTo(MIN_RATE) < 0) {
            return MIN_RATE;
        }
        if (normalized.compareTo(MAX_RATE) > 0) {
            return MAX_RATE;
        }
        return normalized;
    }

    private int normalizeSlaScore(Integer score) {
        if (score == null) {
            return DEFAULT_SLA_SCORE;
        }
        return Math.max(0, Math.min(100, score));
    }
}
