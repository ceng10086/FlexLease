package com.flexlease.user.domain;

import com.flexlease.common.user.CreditTier;
import com.flexlease.common.user.CreditTierRules;
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
@Table(name = "user_profile", schema = "users")
public class UserProfile {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private UserProfileGender gender;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "credit_score", nullable = false)
    private Integer creditScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "credit_tier", nullable = false, length = 30)
    private CreditTier creditTier;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected UserProfile() {
        // JPA
    }

    private UserProfile(UUID id,
                        UUID userId,
                        String fullName,
                        UserProfileGender gender,
                        String phone,
                        String email,
                        String address,
                        Integer creditScore,
                        CreditTier creditTier) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.creditScore = CreditTierRules.clampScore(creditScore);
        this.creditTier = creditTier == null ? CreditTierRules.tierForScore(this.creditScore) : creditTier;
    }

    public static UserProfile create(UUID userId) {
        return new UserProfile(
                UUID.randomUUID(),
                userId,
                null,
                UserProfileGender.UNKNOWN,
                null,
                null,
                null,
                CreditTierRules.defaultScore(),
                CreditTier.STANDARD
        );
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

    public UUID getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public UserProfileGender getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public CreditTier getCreditTier() {
        return creditTier;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateProfile(String fullName,
                              UserProfileGender gender,
                              String phone,
                              String email,
                              String address) {
        this.fullName = fullName;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }

    public void applyCreditDelta(int delta) {
        int updated = CreditTierRules.clampScore((creditScore == null ? CreditTierRules.defaultScore() : creditScore) + delta);
        setCreditScoreInternal(updated);
    }

    public void refreshCreditTier() {
        setCreditScoreInternal(creditScore == null ? CreditTierRules.defaultScore() : creditScore);
    }

    public void setCreditScore(int newScore) {
        setCreditScoreInternal(CreditTierRules.clampScore(newScore));
    }

    private void setCreditScoreInternal(int newScore) {
        this.creditScore = newScore;
        this.creditTier = CreditTierRules.tierForScore(newScore);
    }
}
