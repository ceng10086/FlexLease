package com.flexlease.common.security;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * 平台统一的登录主体信息。
 *
 * <p>由 JWT 解析得到，并写入 Spring Security 的 {@code Authentication#principal}：
 * - {@code userId}: 当前账号对应的用户 ID（消费者/管理员等）
 * - {@code vendorId}: 若账号绑定了厂商身份则有值
 * - {@code roles}: 角色集合（如 USER/VENDOR/ADMIN/INTERNAL）</p>
 */
public record FlexleasePrincipal(UUID userId, UUID vendorId, String username, Set<String> roles) {

    public Set<String> roles() {
        return roles == null ? Collections.emptySet() : Collections.unmodifiableSet(roles);
    }

    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        return roles().stream().anyMatch(r -> r.equalsIgnoreCase(role));
    }
}
