package com.flexlease.auth.service;

import com.flexlease.auth.config.JwtTokenProvider;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
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

    public String refreshToken(String refreshToken) {
        Claims claims = tokenProvider.parseClaims(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌无效或已过期"));
        String username = claims.get("username", String.class);
        if (username == null || username.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌缺少用户名信息");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!(userDetails instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法刷新令牌，用户信息异常");
        }
        return generateToken(userPrincipal);
    }
}
