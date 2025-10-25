package com.flexlease.notification.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationSendRequest;
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
        return ApiResponse.success(notificationService.sendNotification(request));
    }

    @GetMapping("/logs")
    public ApiResponse<List<NotificationLogResponse>> logs(@RequestParam(required = false) String status) {
        NotificationStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = NotificationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法状态值: " + status);
            }
        }
        return ApiResponse.success(notificationService.listLogs(statusEnum));
    }

    @GetMapping("/templates")
    public ApiResponse<List<NotificationTemplateResponse>> templates() {
        return ApiResponse.success(notificationService.listTemplates());
    }
}
