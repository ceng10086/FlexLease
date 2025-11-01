package com.flexlease.common.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<FlexleasePrincipal> getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof FlexleasePrincipal flexleasePrincipal) {
            return Optional.of(flexleasePrincipal);
        }
        return Optional.empty();
    }

    public static Optional<UUID> getCurrentUserId() {
        return getCurrentPrincipal().flatMap(principal -> Optional.ofNullable(principal.userId()));
    }

    public static boolean hasRole(String role) {
        return getCurrentPrincipal().map(principal -> principal.hasRole(role)).orElse(false);
    }
}
