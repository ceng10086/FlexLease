package com.flexlease.notification.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.notification.domain.NotificationStatus;
import com.flexlease.notification.dto.NotificationLogResponse;
import com.flexlease.notification.dto.NotificationTemplateResponse;
import com.flexlease.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public ApiResponse<NotificationLogResponse> send(@Valid @RequestBody NotificationSendRequest request) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("ADMIN") && !principal.hasRole("INTERNAL")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员或内部服务可发送通知");
        }
        return ApiResponse.success(notificationService.sendNotification(request));
    }

    @GetMapping("/logs")
    public ApiResponse<List<NotificationLogResponse>> logs(@RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String recipient) {
        NotificationStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = NotificationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法状态值: " + status);
            }
        }
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        String normalizedRecipient = recipient != null && !recipient.isBlank() ? recipient : null;
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            return ApiResponse.success(notificationService.listLogs(statusEnum, normalizedRecipient));
        }

        if (principal.hasRole("VENDOR")) {
            if (principal.vendorId() == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少厂商标识");
            }
            String vendorRecipient = principal.vendorId().toString();
            if (normalizedRecipient != null && !normalizedRecipient.equals(vendorRecipient)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "禁止查看其他厂商的通知");
            }
            return ApiResponse.success(notificationService.listLogs(statusEnum, vendorRecipient));
        }

        if (principal.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少用户标识");
        }
        String userRecipient = principal.userId().toString();
        if (normalizedRecipient != null && !normalizedRecipient.equals(userRecipient)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "禁止查看其他用户的通知");
        }
        return ApiResponse.success(notificationService.listLogs(statusEnum, userRecipient));
    }

    @GetMapping("/templates")
    public ApiResponse<List<NotificationTemplateResponse>> templates() {
        return ApiResponse.success(notificationService.listTemplates());
    }
}
