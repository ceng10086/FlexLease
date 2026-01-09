package com.flexlease.user.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.user.dto.CreditAdjustmentRequest;
import com.flexlease.user.dto.UserCreditResponse;
import com.flexlease.user.service.UserProfileService;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

/**
 * 用户内部接口（供微服务之间调用）。
 * <p>
 * 例如订单服务在试算押金/风控时，需要读取用户信用档案；内部接口要求 INTERNAL 角色。
 */
@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final UserProfileService userProfileService;

    public InternalUserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{userId}/credit")
    public ApiResponse<UserCreditResponse> getCredit(@PathVariable String userId) {
        if (!SecurityUtils.hasRole("INTERNAL")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅内部服务可访问信用档案");
        }
        UUID userUuid = parseUuid(userId);
        return ApiResponse.success(userProfileService.loadCredit(userUuid));
    }

    @PostMapping("/{userId}/credit-adjustments")
    public ApiResponse<UserCreditResponse> adjustCredit(@PathVariable String userId,
                                                        @Valid @RequestBody CreditAdjustmentRequest request) {
        if (!SecurityUtils.hasRole("INTERNAL")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅内部服务可调整信用分");
        }
        UUID userUuid = parseUuid(userId);
        var profile = userProfileService.adjustCredit(userUuid, request.delta(), request.reason(), null);
        return ApiResponse.success(new UserCreditResponse(profile.userId(), profile.creditScore(), profile.creditTier()));
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "userId 需为合法 UUID");
        }
    }
}
