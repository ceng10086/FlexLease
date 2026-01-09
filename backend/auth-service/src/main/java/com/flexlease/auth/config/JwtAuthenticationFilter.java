package com.flexlease.auth.config;

import com.flexlease.auth.service.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 鉴权过滤器：从 Authorization Header 解析 Bearer Token，并在成功时写入 SecurityContext。
 *
 * <p>注意：本过滤器只负责“解析+放行”，不会在 Token 无效时主动拦截请求；拦截由
 * {@link org.springframework.security.web.AuthenticationEntryPoint} 统一处理。</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final TokenService tokenService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, TokenService tokenService) {
        this.tokenProvider = tokenProvider;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Optional<Claims> claimsOpt = tokenProvider.parseClaims(token);
            claimsOpt.flatMap(claims -> tokenService.buildAuthentication(claims, token))
                    .ifPresent(authentication -> SecurityContextHolder.getContext().setAuthentication(authentication));
        }
        filterChain.doFilter(request, response);
    }
}
