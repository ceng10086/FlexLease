package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.order.dto.ProofPolicySummary;
import com.flexlease.order.service.ProofPolicyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 取证策略接口：返回发货/收货/退租阶段的取证要求与拍摄指引。
 */
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
