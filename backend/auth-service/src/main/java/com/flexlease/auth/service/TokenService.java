package com.flexlease.auth.service;

import com.flexlease.auth.config.JwtTokenProvider;
import com.flexlease.auth.config.SecurityProperties;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Token 相关业务逻辑：生成 access/refresh、从 claims 构造 Authentication、刷新 token。
 *
 * <p>约定：access token 才会被用于请求鉴权；refresh token 只用于换取新的 token 对。</p>
 */
@Service
public class TokenService {

    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final SecurityProperties securityProperties;

    public TokenService(JwtTokenProvider tokenProvider,
                        UserDetailsService userDetailsService,
                        SecurityProperties securityProperties) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.securityProperties = securityProperties;
    }

    public TokenBundle generateTokens(UserPrincipal principal) {
        String rolesCsv = toRolesCsv(principal);
        String accessToken = tokenProvider.generateAccessToken(
                principal.getUserId(),
                principal.getVendorId(),
                principal.getUsername(),
                rolesCsv
        );
        String refreshToken = tokenProvider.generateRefreshToken(
                principal.getUserId(),
                principal.getVendorId(),
                principal.getUsername(),
                rolesCsv
        );
        return new TokenBundle(
                accessToken,
                securityProperties.getAccessTokenTtlSeconds(),
                refreshToken,
                securityProperties.getRefreshTokenTtlSeconds()
        );
    }

    public Optional<UsernamePasswordAuthenticationToken> buildAuthentication(Claims claims, String token) {
        String tokenType = claims.get("tokenType", String.class);
        if (tokenType != null && !TOKEN_TYPE_ACCESS.equalsIgnoreCase(tokenType)) {
            // refresh token 不参与接口鉴权
            return Optional.empty();
        }
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

    public TokenBundle refreshTokens(String refreshToken) {
        Claims claims = tokenProvider.parseClaims(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌无效或已过期"));
        String tokenType = claims.get("tokenType", String.class);
        if (tokenType == null || !TOKEN_TYPE_REFRESH.equalsIgnoreCase(tokenType)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌类型无效");
        }
        String username = claims.get("username", String.class);
        if (username == null || username.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌缺少用户名信息");
        }
        // refresh token 本身的角色/venderId 可能已经过时，这里以数据库加载的最新信息重新签发
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!(userDetails instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法刷新令牌，用户信息异常");
        }
        return generateTokens(userPrincipal);
    }

    private String toRolesCsv(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(grantedAuthority -> {
                    String authority = grantedAuthority.getAuthority();
                    return authority != null && authority.startsWith("ROLE_")
                            ? authority.substring(5)
                            : authority;
                })
                .filter(authority -> authority != null && !authority.isBlank())
                .collect(Collectors.joining(","));
    }

    public record TokenBundle(String accessToken,
                              long accessTokenTtlSeconds,
                              String refreshToken,
                              long refreshTokenTtlSeconds) {
    }
}
