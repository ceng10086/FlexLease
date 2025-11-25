package com.flexlease.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationChannel;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.notification.domain.NotificationLog;
import com.flexlease.notification.domain.NotificationStatus;
import com.flexlease.notification.domain.NotificationTemplate;
import com.flexlease.notification.dto.NotificationLogResponse;
import com.flexlease.notification.dto.NotificationTemplateResponse;
import com.flexlease.notification.repository.NotificationLogRepository;
import com.flexlease.notification.repository.NotificationTemplateRepository;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class NotificationService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationTemplateProvider templateProvider;
    private final NotificationLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationTemplateRepository templateRepository,
                               NotificationLogRepository logRepository,
                               ObjectMapper objectMapper,
                               NotificationTemplateProvider templateProvider) {
        this.templateRepository = templateRepository;
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
        this.templateProvider = templateProvider;
    }

    public NotificationLogResponse sendNotification(NotificationSendRequest request) {
        NotificationTemplate template = null;
        if (request.hasTemplate()) {
            template = templateProvider.findByCode(request.templateCode())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "通知模板不存在"));
        }

        NotificationChannel channel = determineChannel(request, template);
        String subject = resolveSubject(request, template);
        String content = resolveContent(request, template);

        if (subject == null || subject.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "通知标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "通知内容不能为空");
        }

        String payload = serializePayload(Optional.ofNullable(request.variables()).orElse(Collections.emptyMap()));

        NotificationLog log = NotificationLog.draft(
                template != null ? template.getCode() : null,
                channel,
                request.recipient(),
                subject,
                content,
            payload,
            normalizeContextType(request.contextType()),
            normalizeContextReference(request.contextReference())
        );

        try {
            simulateDelivery(channel, request.recipient());
            log.markSent();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.markFailed(ex.getMessage());
        }

        NotificationLog saved = logRepository.save(log);
        return toResponse(saved);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<NotificationLogResponse> listLogs(NotificationStatus status, String recipient) {
        return listLogs(status, recipient, null);
    }

    public List<NotificationLogResponse> listLogs(NotificationStatus status, String recipient, String contextType) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        boolean hasGlobalAccess = principal.hasRole("ADMIN") || principal.hasRole("INTERNAL");
        String normalizedRecipient = recipient != null && !recipient.isBlank() ? recipient : null;
        String normalizedContextType = normalizeContextType(contextType);

        if (!hasGlobalAccess) {
            if (principal.hasRole("VENDOR")) {
                if (principal.vendorId() == null) {
                    throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少厂商标识");
                }
                String vendorRecipient = principal.vendorId().toString();
                if (normalizedRecipient == null) {
                    normalizedRecipient = vendorRecipient;
                } else if (!normalizedRecipient.equals(vendorRecipient)) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "禁止查看其他厂商的通知");
                }
            } else {
                if (principal.userId() == null) {
                    throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少用户标识");
                }
                String userRecipient = principal.userId().toString();
                if (normalizedRecipient == null) {
                    normalizedRecipient = userRecipient;
                } else if (!normalizedRecipient.equals(userRecipient)) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "禁止查看其他用户的通知");
                }
            }
        }

        List<NotificationLog> logs = logRepository.findLatest(normalizedRecipient, status, normalizedContextType, PageRequest.of(0, 50));
        return logs.stream().map(this::toResponse).toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<NotificationTemplateResponse> listTemplates() {
        return templateRepository.findAll().stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(template -> new NotificationTemplateResponse(
                        template.getId(),
                        template.getCode(),
                        template.getChannel(),
                        template.getSubject(),
                        template.getContent(),
                        template.getCreatedAt()
                ))
                .toList();
    }

    private NotificationChannel determineChannel(NotificationSendRequest request, NotificationTemplate template) {
        if (request.channel() != null) {
            return request.channel();
        }
        if (template != null) {
            return template.getChannel();
        }
        return NotificationChannel.IN_APP;
    }

    private String resolveSubject(NotificationSendRequest request, NotificationTemplate template) {
        String base = request.subject();
        if ((base == null || base.isBlank()) && template != null) {
            base = template.getSubject();
        }
        return render(base, request.variables());
    }

    private String resolveContent(NotificationSendRequest request, NotificationTemplate template) {
        String base = request.content();
        if ((base == null || base.isBlank()) && template != null) {
            base = template.getContent();
        }
        return render(base, request.variables());
    }

    private String render(String source, Map<String, Object> variables) {
        if (source == null) {
            return null;
        }
        if (variables == null || variables.isEmpty()) {
            return source;
        }
        String rendered = source;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            rendered = rendered.replace(placeholder, entry.getValue() == null ? "" : entry.getValue().toString());
        }
        return rendered;
    }

    private String serializePayload(Map<String, Object> payload) {
        if (payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "通知载荷序列化失败");
        }
    }

    private void simulateDelivery(NotificationChannel channel, String recipient) {
        if (channel == NotificationChannel.EMAIL && !recipient.contains("@")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "邮箱地址格式不正确");
        }
    }

    private NotificationLogResponse toResponse(NotificationLog log) {
        return new NotificationLogResponse(
                log.getId(),
                log.getTemplateCode(),
                log.getChannel(),
                log.getRecipient(),
                log.getSubject(),
                log.getContent(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getContextType(),
                log.getContextReference(),
                log.getSentAt(),
                log.getCreatedAt()
        );
    }

    private String normalizeContextType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeContextReference(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }
}
