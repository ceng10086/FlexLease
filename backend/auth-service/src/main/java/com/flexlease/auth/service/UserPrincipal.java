package com.flexlease.auth.service;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final UUID vendorId;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final Set<SimpleGrantedAuthority> authorities;

    public UserPrincipal(UUID userId, UUID vendorId, String username, String passwordHash, boolean enabled, Set<SimpleGrantedAuthority> authorities) {
        this.userId = userId;
        this.vendorId = vendorId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
