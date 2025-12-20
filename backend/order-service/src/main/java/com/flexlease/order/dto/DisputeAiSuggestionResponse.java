package com.flexlease.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.flexlease.order.domain.DisputeResolutionOption;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DisputeAiSuggestionResponse(
        String summary,
        List<String> keyFacts,
        List<MissingEvidenceItem> missingEvidence,
        RecommendedDecision recommendedDecision,
        DraftMessages draftMessages,
        List<String> riskNotes,
        String model,
        OffsetDateTime generatedAt
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MissingEvidenceItem(
            String who,
            String need,
            String why
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecommendedDecision(
            DisputeResolutionOption option,
            Integer creditDelta,
            Boolean maliciousBehavior,
            String rationale
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DraftMessages(
            String toUser,
            String toVendor
    ) {
    }
}

