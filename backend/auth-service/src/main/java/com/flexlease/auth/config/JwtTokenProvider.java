package com.flexlease.auth.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecurityProperties properties;
    private final Key key;

    public JwtTokenProvider(SecurityProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(ensureBase64(properties.getSecret())));
    }

    public String generateAccessToken(UUID userId, UUID vendorId, String username, String rolesCsv) {
        return generateToken(userId, vendorId, username, rolesCsv, properties.getAccessTokenTtlSeconds(), TokenType.ACCESS);
    }

    public String generateRefreshToken(UUID userId, UUID vendorId, String username, String rolesCsv) {
        return generateToken(userId, vendorId, username, rolesCsv, properties.getRefreshTokenTtlSeconds(), TokenType.REFRESH);
    }

    public Optional<Claims> parseClaims(String token) {
        try {
                Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Optional.of(claims);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String ensureBase64(String secret) {
        try {
            Decoders.BASE64.decode(secret);
            return secret;
        } catch (RuntimeException ex) {
            return java.util.Base64.getEncoder().encodeToString(secret.getBytes());
        }
    }

    private String generateToken(UUID userId,
                                 UUID vendorId,
                                 String username,
                                 String rolesCsv,
                                 long ttlSeconds,
                                 TokenType tokenType) {
        Instant now = Instant.now();
        long effectiveTtl = ttlSeconds > 0 ? ttlSeconds : properties.getAccessTokenTtlSeconds();
        Instant expiry = now.plusSeconds(effectiveTtl);
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        if (rolesCsv != null) {
            claims.put("roles", rolesCsv);
        }
        claims.put("tokenType", tokenType.name());
        if (vendorId != null) {
            claims.put("vendorId", vendorId.toString());
        }
        return Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .addClaims(claims)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private enum TokenType {
        ACCESS,
        REFRESH
    }
}
