package com.flexlease.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_dispute", schema = "order")
public class OrderDispute {

    private static final Duration ESCALATION_WINDOW = Duration.ofHours(48);

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderDisputeStatus status;

    @Column(name = "initiator_id", nullable = false)
    private UUID initiatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiator_role", nullable = false, length = 20)
    private OrderActorRole initiatorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiator_option", nullable = false, length = 40)
    private DisputeResolutionOption initiatorOption;

    @Column(name = "initiator_reason")
    private String initiatorReason;

    @Column(name = "initiator_remark")
    private String initiatorRemark;

    @Column(name = "respondent_id")
    private UUID respondentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "respondent_role", length = 20)
    private OrderActorRole respondentRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "respondent_option", length = 40)
    private DisputeResolutionOption respondentOption;

    @Column(name = "respondent_remark")
    private String respondentRemark;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "initiator_phone_memo", length = 1000)
    private String initiatorPhoneMemo;

    @Column(name = "respondent_phone_memo", length = 1000)
    private String respondentPhoneMemo;

    @Column(name = "initiator_attachment_ids")
    private String initiatorAttachmentIds;

    @Column(name = "respondent_attachment_ids")
    private String respondentAttachmentIds;

    @Column(name = "deadline_at")
    private OffsetDateTime deadlineAt;

    @Column(name = "escalated_by")
    private UUID escalatedBy;

    @Column(name = "escalated_at")
    private OffsetDateTime escalatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_decision_option", length = 40)
    private DisputeResolutionOption adminDecisionOption;

    @Column(name = "admin_decision_remark")
    private String adminDecisionRemark;

    @Column(name = "admin_decision_by")
    private UUID adminDecisionBy;

    @Column(name = "admin_decision_at")
    private OffsetDateTime adminDecisionAt;

    @Column(name = "user_credit_delta")
    private Integer userCreditDelta;

    @Column(name = "appeal_count", nullable = false)
    private int appealCount;

    @Column(name = "countdown_notified_at")
    private OffsetDateTime countdownNotifiedAt;

    @Column(name = "countdown_reminder_level", nullable = false)
    private int countdownReminderLevel;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected OrderDispute() {
        // JPA 需要无参构造
    }

    private OrderDispute(OrderActorRole initiatorRole,
                         UUID initiatorId,
                         DisputeResolutionOption initiatorOption,
                         String initiatorReason,
                         String initiatorRemark) {
        this.id = UUID.randomUUID();
        this.status = OrderDisputeStatus.OPEN;
        this.initiatorRole = initiatorRole;
        this.initiatorId = initiatorId;
        this.initiatorOption = initiatorOption;
        this.initiatorReason = initiatorReason;
        this.initiatorRemark = initiatorRemark;
        this.deadlineAt = OffsetDateTime.now().plus(ESCALATION_WINDOW);
    }

    public static OrderDispute create(OrderActorRole initiatorRole,
                                      UUID initiatorId,
                                      DisputeResolutionOption initiatorOption,
                                      String initiatorReason,
                                      String initiatorRemark) {
        return new OrderDispute(initiatorRole, initiatorId, initiatorOption, initiatorReason, initiatorRemark);
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public RentalOrder getOrder() {
        return order;
    }

    public void setOrder(RentalOrder order) {
        this.order = order;
    }

    public OrderDisputeStatus getStatus() {
        return status;
    }

    public UUID getInitiatorId() {
        return initiatorId;
    }

    public OrderActorRole getInitiatorRole() {
        return initiatorRole;
    }

    public DisputeResolutionOption getInitiatorOption() {
        return initiatorOption;
    }

    public String getInitiatorReason() {
        return initiatorReason;
    }

    public String getInitiatorRemark() {
        return initiatorRemark;
    }

    public String getInitiatorPhoneMemo() {
        return initiatorPhoneMemo;
    }

    public void setInitiatorPhoneMemo(String phoneMemo) {
        this.initiatorPhoneMemo = phoneMemo;
    }

    public java.util.List<java.util.UUID> getInitiatorAttachmentProofIds() {
        return decodeAttachmentIds(initiatorAttachmentIds);
    }

    public void setInitiatorAttachments(java.util.List<java.util.UUID> attachmentIds) {
        this.initiatorAttachmentIds = encodeAttachmentIds(attachmentIds);
    }

    public UUID getRespondentId() {
        return respondentId;
    }

    public OrderActorRole getRespondentRole() {
        return respondentRole;
    }

    public DisputeResolutionOption getRespondentOption() {
        return respondentOption;
    }

    public String getRespondentRemark() {
        return respondentRemark;
    }

    public String getRespondentPhoneMemo() {
        return respondentPhoneMemo;
    }

    public void setRespondentPhoneMemo(String phoneMemo) {
        this.respondentPhoneMemo = phoneMemo;
    }

    public java.util.List<java.util.UUID> getRespondentAttachmentProofIds() {
        return decodeAttachmentIds(respondentAttachmentIds);
    }

    public void setRespondentAttachments(java.util.List<java.util.UUID> attachmentIds) {
        this.respondentAttachmentIds = encodeAttachmentIds(attachmentIds);
    }

    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    public OffsetDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public UUID getEscalatedBy() {
        return escalatedBy;
    }

    public OffsetDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public DisputeResolutionOption getAdminDecisionOption() {
        return adminDecisionOption;
    }

    public String getAdminDecisionRemark() {
        return adminDecisionRemark;
    }

    public UUID getAdminDecisionBy() {
        return adminDecisionBy;
    }

    public OffsetDateTime getAdminDecisionAt() {
        return adminDecisionAt;
    }

    public Integer getUserCreditDelta() {
        return userCreditDelta;
    }

    public int getAppealCount() {
        return appealCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getCountdownNotifiedAt() {
        return countdownNotifiedAt;
    }

    public void recordResponse(OrderActorRole role,
                               UUID responderId,
                               DisputeResolutionOption option,
                               String remark,
                               boolean accept) {
        if (status != OrderDisputeStatus.OPEN) {
            throw new IllegalStateException("当前纠纷状态不允许回应");
        }
        this.respondentRole = role;
        this.respondentId = responderId;
        this.respondentOption = option;
        this.respondentRemark = remark;
        this.respondedAt = OffsetDateTime.now();
        if (accept) {
            this.status = OrderDisputeStatus.RESOLVED;
        } else {
            // 保持开启状态，留给双方继续沟通
            this.status = OrderDisputeStatus.OPEN;
            this.deadlineAt = OffsetDateTime.now().plus(ESCALATION_WINDOW);
        }
    }

    public void escalate(UUID actorId) {
        if (status == OrderDisputeStatus.CLOSED) {
            throw new IllegalStateException("纠纷已结案，无法升级");
        }
        this.status = OrderDisputeStatus.PENDING_ADMIN;
        this.escalatedBy = actorId;
        this.escalatedAt = OffsetDateTime.now();
    }

    public void appeal(UUID actorId) {
        if (status != OrderDisputeStatus.CLOSED) {
            throw new IllegalStateException("仅结案后的纠纷可申诉");
        }
        if (appealCount >= 1) {
            throw new IllegalStateException("该纠纷申诉次数已用完");
        }
        this.appealCount += 1;
        // 申诉后进入复核组处理，而非普通管理员
        this.status = OrderDisputeStatus.PENDING_REVIEW_PANEL;
        this.escalatedBy = actorId;
        this.escalatedAt = OffsetDateTime.now();
        this.adminDecisionOption = null;
        this.adminDecisionRemark = null;
        this.adminDecisionBy = null;
        this.adminDecisionAt = null;
        this.userCreditDelta = null;
    }


    public void resolveByAdmin(DisputeResolutionOption decision,
                               String remark,
                               UUID adminId,
                               Integer creditDelta) {
        this.status = OrderDisputeStatus.CLOSED;
        this.adminDecisionOption = decision;
        this.adminDecisionRemark = remark;
        this.adminDecisionBy = adminId;
        this.adminDecisionAt = OffsetDateTime.now();
        this.userCreditDelta = creditDelta;
    }

    public boolean markCountdownNotified() {
        if (this.countdownNotifiedAt != null) {
            return false;
        }
        this.countdownNotifiedAt = OffsetDateTime.now();
        return true;
    }

    /**
     * 获取当前提醒级别。
     */
    public int getCountdownReminderLevel() {
        return countdownReminderLevel;
    }

    /**
     * 尝试推进到下一阶段提醒。
     * @param targetLevel 目标提醒级别（1=24小时提醒, 2=6小时提醒, 3=1小时提醒）
     * @return 如果成功推进返回 true
     */
    public boolean advanceReminderLevel(int targetLevel) {
        if (this.countdownReminderLevel >= targetLevel) {
            return false;
        }
        this.countdownReminderLevel = targetLevel;
        this.countdownNotifiedAt = OffsetDateTime.now();
        return true;
    }

    private static String encodeAttachmentIds(java.util.List<java.util.UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream()
                .map(java.util.UUID::toString)
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static java.util.List<java.util.UUID> decodeAttachmentIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return java.util.Collections.emptyList();
        }
        String[] parts = raw.split(",");
        java.util.List<java.util.UUID> result = new java.util.ArrayList<>(parts.length);
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            try {
                result.add(java.util.UUID.fromString(part.trim()));
            } catch (IllegalArgumentException ignored) {
                // 跳过格式异常的条目，避免影响剩余 id 解析
            }
        }
        return java.util.Collections.unmodifiableList(result);
    }
}
