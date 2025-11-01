package com.flexlease.user.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.user.dto.PagedResponse;
import com.flexlease.user.dto.UserProfileResponse;
import com.flexlease.user.dto.UserStatusUpdateRequest;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.user.integration.AuthServiceClient;
import com.flexlease.user.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserProfileService userProfileService;
    private final AuthServiceClient authServiceClient;

    public AdminUserController(UserProfileService userProfileService,
                               AuthServiceClient authServiceClient) {
        this.userProfileService = userProfileService;
        this.authServiceClient = authServiceClient;
    }

    @GetMapping
    public ApiResponse<PagedResponse<UserProfileResponse>> listUsers(@RequestParam(required = false) String keyword,
                                                                     @RequestParam(defaultValue = "1") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        if (!SecurityUtils.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可查看用户列表");
        }
        return ApiResponse.success(userProfileService.list(keyword, page, size));
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable String userId,
                                              @Valid @RequestBody UserStatusUpdateRequest request) {
        if (!SecurityUtils.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可更新用户状态");
        }
        UUID userUuid = parseUuid(userId, "userId");
        String status = request.status().toUpperCase(Locale.ROOT);
        authServiceClient.updateAccountStatus(userUuid, status);
        return ApiResponse.success();
    }

    private UUID parseUuid(String raw, String fieldName) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + " 需为合法 UUID");
        }
    }
}
