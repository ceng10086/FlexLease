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
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知 API（站内信）。
 * <p>
 * 约定：
 * <ul>
 *   <li>发送通知仅允许 ADMIN/INTERNAL（避免前端或普通用户滥发）。</li>
 *   <li>查询日志时会基于当前 JWT 角色做可见性收敛：用户只能看自己的 userId，厂商只能看自己的 vendorId。</li>
 * </ul>
 */
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
                                                           @RequestParam(required = false) String recipient,
                                                           @RequestParam(required = false) String contextType) {
        NotificationStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = NotificationStatus.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法状态值: " + status);
            }
        }
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        String normalizedRecipient = recipient != null && !recipient.isBlank() ? recipient : null;
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            return ApiResponse.success(notificationService.listLogs(statusEnum, normalizedRecipient, contextType));
        }

        if (principal.hasRole("VENDOR")) {
            if (principal.vendorId() == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少厂商标识");
            }
            String vendorRecipient = principal.vendorId().toString();
            if (normalizedRecipient != null && !normalizedRecipient.equals(vendorRecipient)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "禁止查看其他厂商的通知");
            }
            return ApiResponse.success(notificationService.listLogs(statusEnum, vendorRecipient, contextType));
        }

        if (principal.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少用户标识");
        }
        String userRecipient = principal.userId().toString();
        if (normalizedRecipient != null && !normalizedRecipient.equals(userRecipient)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "禁止查看其他用户的通知");
        }
        return ApiResponse.success(notificationService.listLogs(statusEnum, userRecipient, contextType));
    }

    @GetMapping("/templates")
    public ApiResponse<List<NotificationTemplateResponse>> templates() {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("ADMIN") && !principal.hasRole("INTERNAL")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员或内部服务可查看通知模板");
        }
        return ApiResponse.success(notificationService.listTemplates());
    }
}
