package com.flexlease.auth.controller;

import com.flexlease.auth.config.JwtTokenProvider;
import com.flexlease.auth.config.SecurityProperties;
import com.flexlease.auth.domain.UserAccount;
import com.flexlease.auth.dto.LoginRequest;
import com.flexlease.auth.dto.RegisterRequest;
import com.flexlease.auth.dto.TokenResponse;
import com.flexlease.auth.dto.UserSummary;
import com.flexlease.auth.service.AuthService;
import com.flexlease.auth.service.RoleService;
import com.flexlease.auth.service.TokenService;
import com.flexlease.auth.service.UserAccountService;
import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final JwtTokenProvider tokenProvider;
    private final SecurityProperties securityProperties;

    public AuthController(AuthService authService,
                          TokenService tokenService,
                          JwtTokenProvider tokenProvider,
                          SecurityProperties securityProperties) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.tokenProvider = tokenProvider;
        this.securityProperties = securityProperties;
    }

    @PostMapping("/register/customer")
    public ApiResponse<UserSummary> registerCustomer(@Valid @RequestBody RegisterRequest request) {
        UserAccount account = authService.registerCustomer(request.username(), request.password());
        return ApiResponse.success(toSummary(account, Set.of(RoleService.ROLE_USER)));
    }

    @PostMapping("/register/vendor")
    public ApiResponse<UserSummary> registerVendor(@Valid @RequestBody RegisterRequest request) {
        UserAccount account = authService.registerVendor(request.username(), request.password());
        return ApiResponse.success(toSummary(account, Set.of(RoleService.ROLE_VENDOR)));
    }

    @PostMapping("/token")
    public ApiResponse<TokenResponse> token(@Valid @RequestBody LoginRequest request) {
        String accessToken = authService.authenticate(request.username(), request.password());
        return ApiResponse.success(new TokenResponse(accessToken, securityProperties.getAccessTokenTtlSeconds()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummary>> me(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        if (userDetails == null || authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.failure(ErrorCode.UNAUTHORIZED.code(), "未认证"));
        }
        String token = authorizationHeader.substring(7);
        Optional<Claims> claims = tokenProvider.parseClaims(token);
        if (claims.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.failure(ErrorCode.UNAUTHORIZED.code(), "令牌无效"));
        }
        Set<String> roles = tokenService.extractRoles(claims.get());
        UserSummary summary = new UserSummary(
                java.util.UUID.fromString(claims.get().getSubject()),
                userDetails.getUsername(),
                roles,
                null
        );
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    private UserSummary toSummary(UserAccount account, Set<String> roles) {
        return new UserSummary(account.getId(), account.getUsername(), roles, account.getLastLoginAt());
    }
}
