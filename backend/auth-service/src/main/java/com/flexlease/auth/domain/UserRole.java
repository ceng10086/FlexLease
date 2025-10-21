package com.flexlease.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_role", schema = "auth")
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @Column(name = "granted_at", nullable = false)
    private OffsetDateTime grantedAt;

    protected UserRole() {
        // JPA
    }

    private UserRole(UserRoleId id, OffsetDateTime grantedAt) {
        this.id = id;
        this.grantedAt = grantedAt;
    }

    public static UserRole of(UserRoleId id) {
        return new UserRole(id, OffsetDateTime.now());
    }

    public UserRoleId getId() {
        return id;
    }

    public OffsetDateTime getGrantedAt() {
        return grantedAt;
    }
}
