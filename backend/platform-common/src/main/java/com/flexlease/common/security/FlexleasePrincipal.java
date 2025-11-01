package com.flexlease.common.security;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public record FlexleasePrincipal(UUID userId, String username, Set<String> roles) {

    public Set<String> roles() {
        return roles == null ? Collections.emptySet() : Collections.unmodifiableSet(roles);
    }

    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        return roles().stream().anyMatch(r -> r.equalsIgnoreCase(role));
    }
}
