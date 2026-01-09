package com.flexlease.auth.service;

import com.flexlease.auth.domain.Role;
import com.flexlease.auth.domain.UserAccount;
import com.flexlease.auth.domain.UserRole;
import com.flexlease.auth.domain.UserStatus;
import com.flexlease.auth.repository.UserAccountRepository;
import com.flexlease.auth.repository.RoleRepository;
import com.flexlease.auth.repository.UserRoleRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security 用户加载逻辑：根据用户名查账号、校验状态、加载角色并转换为 GrantedAuthority。
 *
 * <p>约定：角色在 token 中以 `ROLE_` 前缀的 authority 形式参与 RBAC。</p>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public CustomUserDetailsService(UserAccountRepository userAccountRepository,
                                    UserRoleRepository userRoleRepository,
                                    RoleRepository roleRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        if (account.getStatus() == UserStatus.DISABLED) {
            throw new DisabledException("账号已被禁用");
        }
        // 先查关联表拿到 roleId，再批量查角色表，最后拼成 ROLE_xxx 的 Spring Security authority
        Set<java.util.UUID> roleIds = userRoleRepository.findByIdUserId(account.getId())
            .stream()
            .map(UserRole::getId)
            .map(com.flexlease.auth.domain.UserRoleId::getRoleId)
            .collect(Collectors.toSet());
        List<Role> roles = roleRepository.findAllById(roleIds);
        Set<SimpleGrantedAuthority> authorities = roles.stream()
            .map(Role::getCode)
            .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
            .collect(Collectors.toSet());
        boolean enabled = account.getStatus() != UserStatus.DISABLED;
        return new UserPrincipal(account.getId(), account.getVendorId(), account.getUsername(), account.getPasswordHash(), enabled, authorities);
    }
}
