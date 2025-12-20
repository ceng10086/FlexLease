package com.flexlease.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.order.client.DeepSeekChatClient;
import com.flexlease.order.client.DeepSeekChatClient.ChatMessage;
import com.flexlease.order.config.LlmProperties;
import com.flexlease.order.domain.DisputeResolutionOption;
import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderDispute;
import com.flexlease.order.domain.OrderEvent;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.OrderProof;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.dto.DisputeAiSuggestionRequest;
import com.flexlease.order.dto.DisputeAiSuggestionResponse;
import com.flexlease.order.dto.ProofPolicySummary;
import com.flexlease.order.repository.DisputeAiSuggestionRepository;
import com.flexlease.order.repository.OrderDisputeRepository;
import com.flexlease.order.repository.RentalOrderRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class DisputeAiSuggestionService {

    private static final Logger LOG = LoggerFactory.getLogger(DisputeAiSuggestionService.class);
    private static final String PROMPT_VERSION = "v1";

    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\b\\d{3})\\d{4}(\\d{4}\\b)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([A-Za-z0-9._%+-]{1,3})[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    private final RentalOrderRepository rentalOrderRepository;
    private final OrderDisputeRepository orderDisputeRepository;
    private final DisputeAiSuggestionRepository suggestionRepository;
    private final ProofPolicyService proofPolicyService;
    private final DeepSeekChatClient chatClient;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final OrderTimelineService timelineService;

    public DisputeAiSuggestionService(RentalOrderRepository rentalOrderRepository,
                                     OrderDisputeRepository orderDisputeRepository,
                                     DisputeAiSuggestionRepository suggestionRepository,
                                     ProofPolicyService proofPolicyService,
                                     DeepSeekChatClient chatClient,
                                     LlmProperties llmProperties,
                                     ObjectMapper objectMapper,
                                     OrderTimelineService timelineService) {
        this.rentalOrderRepository = rentalOrderRepository;
        this.orderDisputeRepository = orderDisputeRepository;
        this.suggestionRepository = suggestionRepository;
        this.proofPolicyService = proofPolicyService;
        this.chatClient = chatClient;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
        this.timelineService = timelineService;
    }

    public DisputeAiSuggestionResponse generate(UUID orderId, UUID disputeId, DisputeAiSuggestionRequest request) {
        SecurityUtils.requireRole("ADMIN");
        RentalOrder order = loadOrder(orderId);
        OrderDispute dispute = loadDispute(orderId, disputeId);

        boolean force = request != null && Boolean.TRUE.equals(request.force());
        if (!force) {
            var existing = suggestionRepository.findByDisputeId(disputeId).orElse(null);
            if (existing != null) {
                return parseStored(existing.getOutputJson(), existing.getModel(), existing.getCreatedAt());
            }
        }

        String tone = normalizeTone(request == null ? null : request.tone());
        ProofPolicySummary policy = proofPolicyService.getPolicy();
        Map<String, Object> llmInput = buildLlmInput(order, dispute, policy);
        String inputJson = writeJson(llmInput);
        String inputHash = sha256Hex(inputJson);

        String suggestionJson = generateSuggestionJsonStrict(tone, inputJson);
        DisputeAiSuggestionResponse parsed = parseSuggestion(suggestionJson);
        DisputeAiSuggestionResponse enriched = new DisputeAiSuggestionResponse(
                parsed.summary(),
                parsed.keyFacts(),
                parsed.missingEvidence(),
                normalizeDecision(parsed.recommendedDecision()),
                parsed.draftMessages(),
                parsed.riskNotes(),
                llmProperties.getModel(),
                OffsetDateTime.now()
        );

        persistSuggestion(order, disputeId, tone, inputHash, suggestionJson);
        timelineService.append(
                order,
                OrderEventType.DISPUTE_AI_SUGGESTED,
                "AI 仲裁建议已生成",
                SecurityUtils.requireUserId(),
                Map.of("disputeId", disputeId.toString(), "promptVersion", PROMPT_VERSION),
                OrderActorRole.ADMIN
        );

        return enriched;
    }

    private String generateSuggestionJsonStrict(String tone, String inputJson) {
        String content = chatClient.createChatCompletion(List.of(
                new ChatMessage("system", buildSystemPrompt()),
                new ChatMessage("user", buildUserPrompt(tone, inputJson))
        ));
        String extracted = extractJsonObject(content);
        validateSuggestionJson(extracted);
        return extracted;
    }

    private void persistSuggestion(RentalOrder order,
                                   UUID disputeId,
                                   String tone,
                                   String inputHash,
                                   String outputJson) {
        UUID adminId = SecurityUtils.requireUserId();
        var entity = suggestionRepository.findByDisputeId(disputeId).orElse(null);
        if (entity == null) {
            entity = com.flexlease.order.domain.DisputeAiSuggestion.create(
                    order,
                    disputeId,
                    llmProperties.getModel(),
                    PROMPT_VERSION,
                    tone,
                    inputHash,
                    outputJson,
                    adminId
            );
            suggestionRepository.save(entity);
            return;
        }
        entity.update(llmProperties.getModel(), PROMPT_VERSION, tone, inputHash, outputJson, adminId);
        suggestionRepository.save(entity);
    }

    private DisputeAiSuggestionResponse parseStored(String json, String model, OffsetDateTime createdAt) {
        DisputeAiSuggestionResponse parsed = parseSuggestion(json);
        return new DisputeAiSuggestionResponse(
                parsed.summary(),
                parsed.keyFacts(),
                parsed.missingEvidence(),
                normalizeDecision(parsed.recommendedDecision()),
                parsed.draftMessages(),
                parsed.riskNotes(),
                model,
                createdAt
        );
    }

    private DisputeAiSuggestionResponse parseSuggestion(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 输出不是 JSON 对象");
            }

            String summary = readText(root, "summary");
            List<String> keyFacts = readTextArray(root.get("keyFacts"));
            List<DisputeAiSuggestionResponse.MissingEvidenceItem> missingEvidence = readMissingEvidence(root.get("missingEvidence"));
            DisputeAiSuggestionResponse.RecommendedDecision recommendedDecision = readRecommendedDecision(root.get("recommendedDecision"));
            DisputeAiSuggestionResponse.DraftMessages draftMessages = readDraftMessages(root.get("draftMessages"));
            List<String> riskNotes = readTextArray(root.get("riskNotes"));

            if (!StringUtils.hasText(summary)) {
                summary = "（AI 未返回摘要）";
            }

            return new DisputeAiSuggestionResponse(
                    summary,
                    keyFacts,
                    missingEvidence,
                    recommendedDecision,
                    draftMessages,
                    riskNotes,
                    null,
                    null
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (JsonProcessingException ex) {
            LOG.warn("Failed to parse LLM JSON: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 输出不是有效 JSON");
        } catch (Exception ex) {
            LOG.warn("Failed to parse LLM JSON: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 输出解析失败");
        }
    }

    private DisputeAiSuggestionResponse.RecommendedDecision normalizeDecision(
            DisputeAiSuggestionResponse.RecommendedDecision decision) {
        if (decision == null) {
            return new DisputeAiSuggestionResponse.RecommendedDecision(DisputeResolutionOption.CUSTOM, null, null, null);
        }
        DisputeResolutionOption option = decision.option() == null ? DisputeResolutionOption.CUSTOM : decision.option();
        Integer creditDelta = decision.creditDelta();
        if (creditDelta != null) {
            creditDelta = Math.max(-30, Math.min(30, creditDelta));
        }
        Boolean malicious = decision.maliciousBehavior();
        return new DisputeAiSuggestionResponse.RecommendedDecision(option, creditDelta, malicious, decision.rationale());
    }

    private String readText(JsonNode root, String field) {
        if (root == null) {
            return null;
        }
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        return node.toString();
    }

    private List<String> readTextArray(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item == null || item.isNull()) {
                continue;
            }
            if (item.isTextual()) {
                result.add(item.asText());
            } else {
                result.add(item.toString());
            }
        }
        return result;
    }

    private List<DisputeAiSuggestionResponse.MissingEvidenceItem> readMissingEvidence(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<DisputeAiSuggestionResponse.MissingEvidenceItem> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item == null || item.isNull() || !item.isObject()) {
                continue;
            }
            String who = readText(item, "who");
            String need = readText(item, "need");
            String why = readText(item, "why");
            if (!StringUtils.hasText(who) && !StringUtils.hasText(need) && !StringUtils.hasText(why)) {
                continue;
            }
            result.add(new DisputeAiSuggestionResponse.MissingEvidenceItem(
                    StringUtils.hasText(who) ? who : "UNKNOWN",
                    need,
                    why
            ));
        }
        return result;
    }

    private DisputeAiSuggestionResponse.DraftMessages readDraftMessages(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            return new DisputeAiSuggestionResponse.DraftMessages(null, null);
        }
        String toUser = readText(node, "toUser");
        String toVendor = readText(node, "toVendor");
        return new DisputeAiSuggestionResponse.DraftMessages(toUser, toVendor);
    }

    private DisputeAiSuggestionResponse.RecommendedDecision readRecommendedDecision(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            return new DisputeAiSuggestionResponse.RecommendedDecision(DisputeResolutionOption.CUSTOM, null, null, null);
        }
        String optionRaw = readText(node, "option");
        DisputeResolutionOption option = mapOption(optionRaw);

        Integer creditDelta = null;
        JsonNode creditNode = node.get("creditDelta");
        if (creditNode != null && !creditNode.isNull()) {
            if (creditNode.canConvertToInt()) {
                creditDelta = creditNode.asInt();
            } else if (creditNode.isTextual()) {
                try {
                    creditDelta = Integer.parseInt(creditNode.asText().trim());
                } catch (NumberFormatException ignored) {
                    creditDelta = null;
                }
            }
        }

        Boolean malicious = null;
        JsonNode maliciousNode = node.get("maliciousBehavior");
        if (maliciousNode != null && !maliciousNode.isNull()) {
            if (maliciousNode.isBoolean()) {
                malicious = maliciousNode.asBoolean();
            } else if (maliciousNode.isTextual()) {
                String raw = maliciousNode.asText().trim().toLowerCase(Locale.ROOT);
                if ("true".equals(raw) || "yes".equals(raw) || "1".equals(raw) || "是".equals(raw)) {
                    malicious = true;
                } else if ("false".equals(raw) || "no".equals(raw) || "0".equals(raw) || "否".equals(raw)) {
                    malicious = false;
                }
            }
        }

        String rationale = readText(node, "rationale");
        return new DisputeAiSuggestionResponse.RecommendedDecision(option, creditDelta, malicious, rationale);
    }

    private DisputeResolutionOption mapOption(String raw) {
        if (!StringUtils.hasText(raw)) {
            return DisputeResolutionOption.CUSTOM;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return DisputeResolutionOption.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            // 兼容历史/自由格式输出
        }
        if (normalized.contains("PARTIAL")) {
            return DisputeResolutionOption.PARTIAL_REFUND;
        }
        if (normalized.contains("REDELIVER") || normalized.contains("RESEND") || normalized.contains("SHIP")) {
            return DisputeResolutionOption.REDELIVER;
        }
        if (normalized.contains("RETURN") || normalized.contains("DEPOSIT")) {
            return DisputeResolutionOption.RETURN_WITH_DEPOSIT_DEDUCTION;
        }
        if (normalized.contains("BUYOUT") || normalized.contains("DISCOUNT")) {
            return DisputeResolutionOption.DISCOUNTED_BUYOUT;
        }
        // 中文关键词兜底
        if (raw.contains("补发") || raw.contains("重发")) {
            return DisputeResolutionOption.REDELIVER;
        }
        if (raw.contains("部分退款") || raw.contains("退一部分")) {
            return DisputeResolutionOption.PARTIAL_REFUND;
        }
        if (raw.contains("退租") || raw.contains("扣押金")) {
            return DisputeResolutionOption.RETURN_WITH_DEPOSIT_DEDUCTION;
        }
        if (raw.contains("买断") || raw.contains("折扣")) {
            return DisputeResolutionOption.DISCOUNTED_BUYOUT;
        }
        return DisputeResolutionOption.CUSTOM;
    }

    private Map<String, Object> buildLlmInput(RentalOrder order, OrderDispute dispute, ProofPolicySummary policy) {
        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> orderSummary = new LinkedHashMap<>();
        orderSummary.put("orderId", order.getId());
        orderSummary.put("orderNo", order.getOrderNo());
        orderSummary.put("status", order.getStatus());
        orderSummary.put("planType", order.getPlanType());
        orderSummary.put("totalAmount", order.getTotalAmount());
        orderSummary.put("depositAmount", order.getDepositAmount());
        orderSummary.put("rentAmount", order.getRentAmount());
        orderSummary.put("leaseStartAt", order.getLeaseStartAt());
        orderSummary.put("leaseEndAt", order.getLeaseEndAt());
        orderSummary.put("userId", order.getUserId());
        orderSummary.put("vendorId", order.getVendorId());
        root.put("order", orderSummary);

        Map<String, Object> disputeSummary = new LinkedHashMap<>();
        disputeSummary.put("disputeId", dispute.getId());
        disputeSummary.put("status", dispute.getStatus());
        disputeSummary.put("initiatorRole", dispute.getInitiatorRole());
        disputeSummary.put("initiatorOption", dispute.getInitiatorOption());
        disputeSummary.put("initiatorReason", redact(dispute.getInitiatorReason()));
        disputeSummary.put("initiatorRemark", redact(dispute.getInitiatorRemark()));
        disputeSummary.put("respondentRole", dispute.getRespondentRole());
        disputeSummary.put("respondentOption", dispute.getRespondentOption());
        disputeSummary.put("respondentRemark", redact(dispute.getRespondentRemark()));
        disputeSummary.put("respondentPhoneMemo", redact(dispute.getRespondentPhoneMemo()));
        disputeSummary.put("deadlineAt", dispute.getDeadlineAt());
        root.put("dispute", disputeSummary);

        List<Map<String, Object>> proofs = new ArrayList<>();
        for (OrderProof proof : order.getProofs()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("proofId", proof.getId());
            item.put("proofType", proof.getProofType());
            item.put("description", redact(proof.getDescription()));
            item.put("contentType", proof.getContentType());
            item.put("fileSize", proof.getFileSize());
            item.put("uploadedAt", proof.getUploadedAt());
            item.put("actorRole", proof.getActorRole());
            proofs.add(item);
        }
        root.put("proofs", proofs);

        List<Map<String, Object>> events = new ArrayList<>();
        for (OrderEvent event : lastRelevantEvents(order.getEvents(), 40)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", event.getEventType());
            item.put("description", redact(event.getDescription()));
            item.put("actorRole", event.getActorRole());
            item.put("createdAt", event.getCreatedAt());
            events.add(item);
        }
        root.put("events", events);

        root.put("proofPolicy", policy);
        root.put("platformRules", Map.of(
                "disputeResolutionOptions", List.of(
                        DisputeResolutionOption.REDELIVER.name(),
                        DisputeResolutionOption.PARTIAL_REFUND.name(),
                        DisputeResolutionOption.RETURN_WITH_DEPOSIT_DEDUCTION.name(),
                        DisputeResolutionOption.DISCOUNTED_BUYOUT.name(),
                        DisputeResolutionOption.CUSTOM.name()
                ),
                "creditDeltaRange", "[-30, 30]，正数表示扣分，负数表示加分",
                "maliciousBehaviorHint", "若判定恶意行为，建议 maliciousBehavior=true，并提示冻结 30 天"
        ));

        return root;
    }

    private List<OrderEvent> lastRelevantEvents(List<OrderEvent> raw, int max) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<OrderEventType> allow = List.of(
                OrderEventType.ORDER_CREATED,
                OrderEventType.PAYMENT_CONFIRMED,
                OrderEventType.ORDER_SHIPPED,
                OrderEventType.ORDER_RECEIVED,
                OrderEventType.RETURN_REQUESTED,
                OrderEventType.RETURN_APPROVED,
                OrderEventType.RETURN_REJECTED,
                OrderEventType.RETURN_COMPLETED,
                OrderEventType.PROOF_UPLOADED,
                OrderEventType.COMMUNICATION_NOTE,
                OrderEventType.DISPUTE_OPENED,
                OrderEventType.DISPUTE_RESPONDED,
                OrderEventType.DISPUTE_ESCALATED,
                OrderEventType.DISPUTE_RESOLVED,
                OrderEventType.SURVEY_INVITED,
                OrderEventType.SURVEY_SUBMITTED
        );
        List<OrderEvent> filtered = raw.stream()
                .filter(e -> e.getEventType() != null && allow.contains(e.getEventType()))
                .toList();
        if (filtered.isEmpty()) {
            filtered = raw;
        }
        int size = filtered.size();
        if (size <= max) {
            return filtered;
        }
        return filtered.subList(size - max, size);
    }

    private String buildSystemPrompt() {
        return """
                You are an "order dispute arbitration assistant" for a rental marketplace.
                The user will provide an input JSON that contains order / dispute / evidence / timeline / policy data.
                Please read it and output a JSON object ONLY (no markdown, no extra text).

                IMPORTANT: The output must be a valid json object. Fill missing fields with empty strings/arrays.
                Do NOT fabricate facts that are not present in the input.

                EXAMPLE JSON OUTPUT:
                {
                  "summary": "Short factual summary that can be verified.",
                  "keyFacts": ["fact1", "fact2"],
                  "missingEvidence": [{"who":"USER","need":"...","why":"..."}],
                  "recommendedDecision": {"option":"REDELIVER","creditDelta":0,"maliciousBehavior":false,"rationale":"..."},
                  "draftMessages": {"toUser":"...","toVendor":"..."},
                  "riskNotes": ["..."]
                }
                """.trim();
    }

    private String buildUserPrompt(String tone, String inputJson) {
        return """
                Please generate the arbitration suggestion in json format. tone=%s

                Constraints:
                - option must be one of input.platformRules.disputeResolutionOptions
                - creditDelta range is [-30,30] (positive=penalty, negative=reward)

                INPUT JSON:
                %s
                """.formatted(tone, inputJson).trim();
    }

    private String normalizeTone(String tone) {
        if (!StringUtils.hasText(tone)) {
            return "NEUTRAL";
        }
        return tone.trim().toUpperCase(Locale.ROOT);
    }

    private String redact(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String sanitized = raw;
        sanitized = PHONE_PATTERN.matcher(sanitized).replaceAll("$1****$2");
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("$1***$2");
        return sanitized;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 输入序列化失败");
        }
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 输入哈希失败");
        }
    }

    private String extractJsonObject(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 输出为空");
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 输出不是有效 JSON");
        }
        String json = content.substring(start, end + 1).trim();
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isObject()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 输出不是 JSON 对象");
            }
            return json;
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 输出不是有效 JSON");
        }
    }

    private void validateSuggestionJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 输出不是 JSON 对象");
            }
            if (!root.hasNonNull("summary")) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 输出缺少 summary");
            }
            JsonNode recommendedDecision = root.get("recommendedDecision");
            if (recommendedDecision == null || !recommendedDecision.isObject()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 输出缺少 recommendedDecision");
            }
            JsonNode option = recommendedDecision.get("option");
            if (option == null || option.isNull() || !option.isTextual()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 输出 recommendedDecision.option 非法");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 输出校验失败");
        }
    }

    private RentalOrder loadOrder(UUID orderId) {
        return rentalOrderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在"));
    }

    private OrderDispute loadDispute(UUID orderId, UUID disputeId) {
        return orderDisputeRepository.findByIdAndOrderId(disputeId, orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "纠纷不存在"));
    }
}
