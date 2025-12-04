package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.order.dto.ProofPolicySummary;
import com.flexlease.order.service.ProofPolicyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/proof-policy")
public class ProofPolicyController {

    private final ProofPolicyService proofPolicyService;

    public ProofPolicyController(ProofPolicyService proofPolicyService) {
        this.proofPolicyService = proofPolicyService;
    }

    @GetMapping
    public ApiResponse<ProofPolicySummary> getPolicy() {
        return ApiResponse.success(proofPolicyService.getPolicy());
    }
}
