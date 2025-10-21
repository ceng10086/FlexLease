package com.flexlease.auth.config;

import com.flexlease.auth.domain.UserStatus;
import com.flexlease.auth.service.RoleService;
import com.flexlease.auth.service.UserAccountService;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleService roleService;
    private final UserAccountService userAccountService;

    @Value("${flexlease.bootstrap.admin.username:admin@flexlease.test}")
    private String adminUsername;

    @Value("${flexlease.bootstrap.admin.password:Admin@123}")
    private String adminPassword;

    public DataInitializer(RoleService roleService, UserAccountService userAccountService) {
        this.roleService = roleService;
        this.userAccountService = userAccountService;
    }

    @PostConstruct
    public void init() {
        roleService.ensureRole(RoleService.ROLE_ADMIN, "平台管理员", "平台全局管理员");
        roleService.ensureRole(RoleService.ROLE_VENDOR, "厂商", "B 端厂商用户");
        roleService.ensureRole(RoleService.ROLE_USER, "消费者", "C 端用户");

        try {
            userAccountService.register(adminUsername, adminPassword, UserStatus.ENABLED,
                    Set.of(RoleService.ROLE_ADMIN));
            log.info("Bootstrap admin account created: {}", adminUsername);
        } catch (com.flexlease.common.exception.BusinessException ex) {
            if (ex.getErrorCode() == com.flexlease.common.exception.ErrorCode.DUPLICATE_RESOURCE) {
                log.debug("Bootstrap admin already exists: {}", adminUsername);
            } else {
                throw ex;
            }
        }
    }
}
