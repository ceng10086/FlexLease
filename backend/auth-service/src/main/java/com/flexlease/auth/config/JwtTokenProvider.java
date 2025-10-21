package com.flexlease.auth.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
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

    public String generateToken(UUID userId, String username, String rolesCsv) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getAccessTokenTtlSeconds());
        return Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .addClaims(Map.of(
                        "username", username,
                        "roles", rolesCsv
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
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
}
