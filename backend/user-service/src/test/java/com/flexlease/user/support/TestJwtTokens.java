package com.flexlease.user.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public final class TestJwtTokens {

    private static final String SECRET = "flexlease-default-secret-please-change";
    private static final String ISSUER = "flexlease-auth-service";

    private TestJwtTokens() {
    }

    public static String bearerToken(UUID userId, String username, String... roles) {
        return bearerToken(userId, null, username, roles);
    }

    public static String bearerToken(UUID userId, UUID vendorId, String username, String... roles) {
        return "Bearer " + token(userId, vendorId, username, roles);
    }

    public static String token(UUID userId, String username, String... roles) {
        return token(userId, null, username, roles);
    }

    public static String token(UUID userId, UUID vendorId, String username, String... roles) {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(ensureBase64(SECRET)));
        String rolesCsv = roles == null || roles.length == 0 ? "" : String.join(",", roles);
        String subject = userId == null ? UUID.randomUUID().toString() : userId.toString();
        var builder = Jwts.builder()
                .setSubject(subject)
                .claim("username", username)
                .claim("roles", rolesCsv)
                .setIssuer(ISSUER)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));
        if (vendorId != null) {
            builder.claim("vendorId", vendorId.toString());
        }
        return builder.signWith(key).compact();
    }

    private static String ensureBase64(String secret) {
        try {
            Decoders.BASE64.decode(secret);
            return secret;
        } catch (RuntimeException ignored) {
            return Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8));
        }
    }
}
