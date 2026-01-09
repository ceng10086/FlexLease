package com.flexlease.common.security;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类。
 *
 * <p>对 {@link org.springframework.security.core.context.SecurityContext} 做轻量封装，
 * 方便业务代码获取当前登录主体并进行角色/身份校验。</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<FlexleasePrincipal> getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof FlexleasePrincipal flexleasePrincipal) {
            return Optional.of(flexleasePrincipal);
        }
        return Optional.empty();
    }

    public static Optional<UUID> getCurrentUserId() {
        return getCurrentPrincipal().flatMap(principal -> Optional.ofNullable(principal.userId()));
    }

    public static Optional<UUID> getCurrentVendorId() {
        return getCurrentPrincipal().flatMap(principal -> Optional.ofNullable(principal.vendorId()));
    }

    public static boolean hasRole(String role) {
        return getCurrentPrincipal().map(principal -> principal.hasRole(role)).orElse(false);
    }

    public static FlexleasePrincipal requirePrincipal() {
        return getCurrentPrincipal().orElseThrow(() ->
                new BusinessException(ErrorCode.UNAUTHORIZED, "未认证"));
    }

    public static UUID requireUserId() {
        FlexleasePrincipal principal = requirePrincipal();
        UUID userId = principal.userId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少用户标识");
        }
        return userId;
    }

    public static UUID requireVendorId() {
        FlexleasePrincipal principal = requirePrincipal();
        UUID vendorId = principal.vendorId();
        if (vendorId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少厂商标识");
        }
        return vendorId;
    }

    public static void requireRole(String role) {
        FlexleasePrincipal principal = requirePrincipal();
        if (!principal.hasRole(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "缺少权限: " + role);
        }
    }

    public static void requireAnyRole(String... roles) {
        FlexleasePrincipal principal = requirePrincipal();
        if (roles == null || roles.length == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "缺少权限");
        }
        for (String role : roles) {
            if (role != null && !role.isBlank() && principal.hasRole(role)) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "缺少权限: " + String.join("/", roles));
    }
}
