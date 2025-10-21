package com.flexlease.auth.service;

import com.flexlease.auth.domain.UserAccount;
import com.flexlease.auth.domain.UserStatus;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountService userAccountService;
    private final TokenService tokenService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserAccountService userAccountService,
                       TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.userAccountService = userAccountService;
        this.tokenService = tokenService;
    }

    @Transactional
    public UserAccount registerCustomer(String username, String password) {
        return userAccountService.register(username, password, UserStatus.ENABLED, Set.of(RoleService.ROLE_USER));
    }

    @Transactional
    public UserAccount registerVendor(String username, String password) {
        return userAccountService.register(username, password, UserStatus.PENDING_REVIEW, Set.of(RoleService.ROLE_VENDOR));
    }

    public String authenticate(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            userAccountService.updateLastLogin(userPrincipal.getUserId());
            return tokenService.generateToken(userPrincipal);
        }
        if (principal instanceof UserDetails userDetails) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "未知的认证主体: " + userDetails.getUsername());
        }
        throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "认证失败");
    }
}
