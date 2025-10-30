package com.flexlease.user.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.user.dto.UserProfileResponse;
import com.flexlease.user.dto.UserProfileUpdateRequest;
import com.flexlease.user.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/v1/customers/profile")
public class CustomerProfileController {

    private final UserProfileService userProfileService;

    public CustomerProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ApiResponse<UserProfileResponse> getProfile(@RequestHeader("X-User-Id") String userIdHeader) {
        UUID userId = parseUuid(userIdHeader, "userId");
        return ApiResponse.success(userProfileService.getOrCreate(userId));
    }

    @PutMapping
    public ApiResponse<UserProfileResponse> updateProfile(@RequestHeader("X-User-Id") String userIdHeader,
                                                          @Valid @RequestBody UserProfileUpdateRequest request) {
        UUID userId = parseUuid(userIdHeader, "userId");
        return ApiResponse.success(userProfileService.update(userId, request));
    }

    private UUID parseUuid(String raw, String fieldName) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + " 需为合法 UUID");
        }
    }
}
