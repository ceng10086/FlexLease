package com.flexlease.order.dto;

import java.util.List;

public record ProofPolicySummary(
        ProofStagePolicy shipment,
        ProofStagePolicy receive,
        ProofStagePolicy returns
) {

    public record ProofStagePolicy(
            int photosRequired,
            int videosRequired,
            List<String> guidance,
            String watermarkExample
    ) {
    }
}
