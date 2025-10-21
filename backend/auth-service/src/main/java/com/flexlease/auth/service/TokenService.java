package com.flexlease.auth.service;

import com.flexlease.auth.config.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    public TokenService(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    public String generateToken(UserPrincipal principal) {
        String rolesCsv = principal.getAuthorities().stream()
            .map(grantedAuthority -> {
                String authority = grantedAuthority.getAuthority();
                return authority != null && authority.startsWith("ROLE_")
                    ? authority.substring(5)
                    : authority;
            })
            .filter(authority -> authority != null && !authority.isBlank())
            .collect(Collectors.joining(","));
        return tokenProvider.generateToken(principal.getUserId(), principal.getUsername(), rolesCsv);
    }

    public Optional<UsernamePasswordAuthenticationToken> buildAuthentication(Claims claims, String token) {
        String username = claims.get("username", String.class);
        if (username == null) {
            return Optional.empty();
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return Optional.of(new UsernamePasswordAuthenticationToken(userDetails, token, userDetails.getAuthorities()));
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public Set<String> extractRoles(Claims claims) {
        String roles = claims.get("roles", String.class);
        if (roles == null || roles.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(roles.split(","))
                .collect(Collectors.toSet());
    }
}
