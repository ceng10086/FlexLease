package com.flexlease.order.dto;

import java.util.List;

/**
 * 取证策略响应 DTO。
 */
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
