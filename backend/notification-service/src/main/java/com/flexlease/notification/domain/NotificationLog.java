package com.flexlease.notification.domain;

import com.flexlease.common.notification.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_log", schema = "notification")
public class NotificationLog {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "template_code", length = 50)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "recipient", nullable = false, length = 100)
    private String recipient;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "payload")
    private String payload;

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
        // JPA
    }

    private NotificationLog(String templateCode,
                             NotificationChannel channel,
                             String recipient,
                             String subject,
                             String content,
                             String payload) {
        this.id = UUID.randomUUID();
        this.templateCode = templateCode;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
        this.payload = payload;
        this.status = NotificationStatus.PENDING;
    }

    public static NotificationLog draft(String templateCode,
                                         NotificationChannel channel,
                                         String recipient,
                                         String subject,
                                         String content,
                                         String payload) {
        return new NotificationLog(templateCode, channel, recipient, subject, content, payload);
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

    public NotificationChannel getChannel() {
        return channel;
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
