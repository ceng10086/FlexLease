package com.flexlease.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 鉴权过滤器。
 * <p>
 * 优先校验内部调用的 {@code X-Internal-Token}，否则校验 {@code Authorization: Bearer <token>}，
 * 并把解析后的身份写入 Spring Security 的 {@link SecurityContextHolder}。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String PRINCIPAL_REQUEST_ATTRIBUTE = "flexlease.principal";

    private final JwtTokenVerifier tokenVerifier;
    private final JwtAuthProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtTokenVerifier tokenVerifier, JwtAuthProperties properties) {
        this.tokenVerifier = tokenVerifier;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return properties.getPermitAll().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authenticateInternalToken(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(response, "缺少访问令牌");
            return;
        }

        String token = authorization.substring(7).trim();
        if (token.isBlank()) {
            writeUnauthorized(response, "访问令牌为空");
            return;
        }

        tokenVerifier.verify(token).ifPresentOrElse(principal -> {
            List<SimpleGrantedAuthority> authorities = principal.roles().stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            User user = new User(principal.username() == null ? principal.userId().toString() : principal.username(),
                    "", authorities);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, token, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute(PRINCIPAL_REQUEST_ATTRIBUTE, principal);
        }, () -> writeUnauthorized(response, "访问令牌无效或已过期"));

        if (!response.isCommitted()) {
            filterChain.doFilter(request, response);
        }
    }

    private boolean authenticateInternalToken(HttpServletRequest request) {
        String internalToken = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (internalToken == null || internalToken.isBlank()) {
            return false;
        }
        if (!internalToken.equals(properties.getInternalAccessToken())) {
            return false;
        }
        FlexleasePrincipal principal = new FlexleasePrincipal(null, null, "internal-service", Set.of("INTERNAL"));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null,
                        Set.of(new SimpleGrantedAuthority("ROLE_INTERNAL")));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute(PRINCIPAL_REQUEST_ATTRIBUTE, principal);
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) {
        if (response.isCommitted()) {
            return;
        }
        try {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            ApiResponse<Void> body = ApiResponse.failure(ErrorCode.UNAUTHORIZED.code(), message);
            response.getWriter().write(objectMapper.writeValueAsString(body));
        } catch (IOException ignored) {
            // 忽略写响应异常
        }
    }
}
