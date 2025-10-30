package com.flexlease.auth.service;

import com.flexlease.auth.domain.UserAccount;
import com.flexlease.auth.domain.UserRole;
import com.flexlease.auth.domain.UserRoleId;
import com.flexlease.auth.domain.UserStatus;
import com.flexlease.auth.repository.UserAccountRepository;
import com.flexlease.auth.repository.UserRoleRepository;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository,
                              UserRoleRepository userRoleRepository,
                              RoleService roleService,
                              PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount register(String username, String password, UserStatus initialStatus, Set<String> roleCodes) {
        userAccountRepository.findByUsernameIgnoreCase(username).ifPresent(acc -> {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "账号已存在");
        });
        String passwordHash = passwordEncoder.encode(password);
        UserAccount account = UserAccount.create(username.toLowerCase(), passwordHash, initialStatus);
        UserAccount saved = userAccountRepository.save(account);
        assignRoles(saved.getId(), roleCodes);
        return saved;
    }

    @Transactional
    public void assignRoles(UUID userId, Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return;
        }
        List<UserRole> roles = roleCodes.stream()
                .map(roleService::getByCode)
                .map(role -> new UserRoleId(userId, role.getId()))
                .map(UserRole::of)
                .toList();
        userRoleRepository.saveAll(roles);
    }

    @Transactional
    public void updateLastLogin(UUID userId) {
        userAccountRepository.findById(userId).ifPresent(account -> {
            account.updateLastLoginAt(OffsetDateTime.now());
        });
    }

    @Transactional
    public void resetPassword(String username, String oldPassword, String newPassword) {
        UserAccount account = userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "账号不存在"));
        if (!passwordEncoder.matches(oldPassword, account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "原密码不正确");
        }
        account.updatePasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public UserAccount updateStatus(UUID userId, UserStatus status) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "账号不存在"));
        account.updateStatus(status);
        return account;
    }
}
