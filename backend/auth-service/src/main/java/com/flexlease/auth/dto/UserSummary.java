package com.flexlease.auth.dto;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record UserSummary(
        UUID id,
        String username,
        Set<String> roles,
        OffsetDateTime lastLoginAt
) {
}
