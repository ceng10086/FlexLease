package com.flexlease.auth.controller;

import com.flexlease.auth.config.SecurityProperties;
import com.flexlease.auth.domain.UserStatus;
import com.flexlease.auth.dto.UpdateUserStatusRequest;
import com.flexlease.auth.dto.UpdateUserVendorRequest;
import com.flexlease.auth.service.UserAccountService;
import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final UserAccountService userAccountService;
    private final SecurityProperties securityProperties;

    public InternalUserController(UserAccountService userAccountService, SecurityProperties securityProperties) {
        this.userAccountService = userAccountService;
        this.securityProperties = securityProperties;
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable("id") UUID userId,
                                                           @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
                                                           @Valid @RequestBody UpdateUserStatusRequest request) {
        validateInternalToken(internalToken);
        UserStatus status = parseStatus(request.status());
        userAccountService.updateStatus(userId, status);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{id}/vendor")
    public ResponseEntity<ApiResponse<Void>> updateVendor(@PathVariable("id") UUID userId,
                                                          @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
                                                          @Valid @RequestBody UpdateUserVendorRequest request) {
        validateInternalToken(internalToken);
        userAccountService.associateVendor(userId, request.vendorId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    private void validateInternalToken(String internalToken) {
        String expected = securityProperties.getInternalAccessToken();
        if (expected == null || expected.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "未配置内部访问令牌");
        }
        if (internalToken == null || !expected.equals(internalToken)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "内部访问令牌无效");
        }
    }

    private UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法状态值: " + status);
        }
    }
}
