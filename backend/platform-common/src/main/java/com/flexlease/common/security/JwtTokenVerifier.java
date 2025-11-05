package com.flexlease.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class JwtTokenVerifier {

    private final String issuer;
    private final Key key;

    public JwtTokenVerifier(JwtAuthProperties properties) {
        this.issuer = properties.getIssuer();
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(ensureBase64(properties.getSecret())));
    }

    public Optional<FlexleasePrincipal> verify(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String tokenType = claims.get("tokenType", String.class);
            if (tokenType != null && !"ACCESS".equalsIgnoreCase(tokenType)) {
                return Optional.empty();
            }
            UUID userId = UUID.fromString(claims.getSubject());
            String username = claims.get("username", String.class);
            UUID vendorId = null;
            Object vendorClaim = claims.get("vendorId");
            if (vendorClaim instanceof String vendorString && !vendorString.isBlank()) {
                vendorId = UUID.fromString(vendorString);
            }
            Set<String> roles = parseRoles(claims.get("roles", String.class));
            return Optional.of(new FlexleasePrincipal(userId, vendorId, username, roles));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Set<String> parseRoles(String rolesCsv) {
        if (rolesCsv == null || rolesCsv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(rolesCsv.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .collect(Collectors.toSet());
    }

    private String ensureBase64(String secret) {
        try {
            Decoders.BASE64.decode(secret);
            return secret;
        } catch (RuntimeException ex) {
            return java.util.Base64.getEncoder().encodeToString(secret.getBytes());
        }
    }
}
