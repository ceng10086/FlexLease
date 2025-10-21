package com.flexlease.auth.service;

import com.flexlease.auth.domain.Role;
import com.flexlease.auth.repository.RoleRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_VENDOR = "VENDOR";
    public static final String ROLE_USER = "USER";

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional
    public Role ensureRole(String code, String name, String description) {
        Optional<Role> existing = roleRepository.findByCode(code);
        if (existing.isPresent()) {
            return existing.get();
        }
        Role role = Role.of(code, name, description);
        return roleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public Role getByCode(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + code));
    }
}
