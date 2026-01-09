package com.flexlease.auth.domain;

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

/**
 * 认证中心账号表实体。
 *
 * <p>说明：vendorId 在厂商入驻审核通过后由 user-service 通过内部接口回写，用于后续 RBAC/数据隔离。</p>
 */
@Entity
@Table(name = "user_account", schema = "auth")
public class UserAccount {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "vendor_id")
    private UUID vendorId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected UserAccount() {
        // JPA 需要无参构造
    }

    private UserAccount(UUID id, String username, String passwordHash, UserStatus status) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
    }

    public static UserAccount create(String username, String passwordHash, UserStatus status) {
        return new UserAccount(UUID.randomUUID(), username, passwordHash, status);
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

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public void updateLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
    }

    public void updateVendorId(UUID vendorId) {
        this.vendorId = vendorId;
    }
}
