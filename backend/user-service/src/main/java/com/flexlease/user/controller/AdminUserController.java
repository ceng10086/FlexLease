package com.flexlease.user.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.user.dto.CreditAdjustmentRequest;
import com.flexlease.user.dto.CreditAdjustmentResponse;
import com.flexlease.user.dto.PagedResponse;
import com.flexlease.user.dto.UserProfileResponse;
import com.flexlease.user.dto.UserStatusUpdateRequest;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.user.integration.AuthServiceClient;
import com.flexlease.user.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员用户管理接口。
 * <p>
 * 包含用户列表、冻结/解冻账号（联动 auth-service）以及信用分人工调整与历史查询。
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminUserController.class);

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

    @PostMapping("/{userId}/credit-adjustments")
    public ApiResponse<UserProfileResponse> adjustCredit(@PathVariable String userId,
                                                         @Valid @RequestBody CreditAdjustmentRequest request) {
        UUID adminId = SecurityUtils.requireUserId();
        if (!SecurityUtils.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可调整信用分");
        }
        UUID userUuid = parseUuid(userId, "userId");
        int delta = request.delta();
        LOG.info("Admin {} adjusting credit for user {} by {}", adminId, userUuid, delta);
        return ApiResponse.success(userProfileService.adjustCredit(userUuid, delta, request.reason(), adminId));
    }

    @GetMapping("/{userId}/credit-adjustments")
    public ApiResponse<PagedResponse<CreditAdjustmentResponse>> listCreditAdjustments(@PathVariable String userId,
                                                                                      @RequestParam(defaultValue = "1") int page,
                                                                                      @RequestParam(defaultValue = "10") int size) {
        if (!SecurityUtils.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可查看信用调整记录");
        }
        UUID userUuid = parseUuid(userId, "userId");
        return ApiResponse.success(userProfileService.listCreditAdjustments(userUuid, page, size));
    }

    private UUID parseUuid(String raw, String fieldName) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + " 需为合法 UUID");
        }
    }
}
