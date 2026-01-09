package com.flexlease.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 通知发送日志（站内信记录）。
 * <p>
 * 当前实现不做“异步真实发送”，保存后会直接标记为 SENT，前端用它作为通知中心的数据来源。
 */
@Entity
@Table(name = "notification_log", schema = "notification")
public class NotificationLog {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "template_code", length = 50)
    private String templateCode;

    @Column(name = "recipient", nullable = false, length = 100)
    private String recipient;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "payload")
    private String payload;

    @Column(name = "context_type", length = 50)
    private String contextType;

    @Column(name = "context_ref", length = 100)
    private String contextReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected NotificationLog() {
        // JPA 需要无参构造
    }

    private NotificationLog(String templateCode,
                             String recipient,
                             String subject,
                             String content,
                             String payload,
                             String contextType,
                             String contextReference) {
        this.id = UUID.randomUUID();
        this.templateCode = templateCode;
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
        this.payload = payload;
        this.contextType = contextType;
        this.contextReference = contextReference;
        this.status = NotificationStatus.PENDING;
    }

    public static NotificationLog draft(String templateCode,
                                         String recipient,
                                         String subject,
                                         String content,
                                         String payload,
                                         String contextType,
                                         String contextReference) {
        return new NotificationLog(templateCode, recipient, subject, content, payload, contextType, contextReference);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = OffsetDateTime.now();
    }

    public void markFailed(String error) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = error;
        this.sentAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public String getPayload() {
        return payload;
    }

    public String getContextType() {
        return contextType;
    }

    public String getContextReference() {
        return contextReference;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
