package com.flexlease.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dispute_ai_suggestion", schema = "order")
public class DisputeAiSuggestion {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder order;

    @Column(name = "dispute_id", nullable = false, unique = true)
    private UUID disputeId;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "prompt_version", length = 40)
    private String promptVersion;

    @Column(name = "tone", length = 20)
    private String tone;

    @Column(name = "input_hash", length = 64)
    private String inputHash;

    @Column(name = "output_json", nullable = false, columnDefinition = "TEXT")
    private String outputJson;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected DisputeAiSuggestion() {
        // JPA
    }

    private DisputeAiSuggestion(RentalOrder order,
                                UUID disputeId,
                                String model,
                                String promptVersion,
                                String tone,
                                String inputHash,
                                String outputJson,
                                UUID createdBy) {
        this.id = UUID.randomUUID();
        this.order = order;
        this.disputeId = disputeId;
        this.model = model;
        this.promptVersion = promptVersion;
        this.tone = tone;
        this.inputHash = inputHash;
        this.outputJson = outputJson;
        this.createdBy = createdBy;
        this.createdAt = OffsetDateTime.now();
    }

    public static DisputeAiSuggestion create(RentalOrder order,
                                            UUID disputeId,
                                            String model,
                                            String promptVersion,
                                            String tone,
                                            String inputHash,
                                            String outputJson,
                                            UUID createdBy) {
        return new DisputeAiSuggestion(order, disputeId, model, promptVersion, tone, inputHash, outputJson, createdBy);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public RentalOrder getOrder() {
        return order;
    }

    public UUID getDisputeId() {
        return disputeId;
    }

    public String getModel() {
        return model;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getTone() {
        return tone;
    }

    public String getInputHash() {
        return inputHash;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void update(String model,
                       String promptVersion,
                       String tone,
                       String inputHash,
                       String outputJson,
                       UUID createdBy) {
        this.model = model;
        this.promptVersion = promptVersion;
        this.tone = tone;
        this.inputHash = inputHash;
        this.outputJson = outputJson;
        this.createdBy = createdBy;
        this.createdAt = OffsetDateTime.now();
    }
}

