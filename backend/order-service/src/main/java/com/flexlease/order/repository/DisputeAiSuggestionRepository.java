package com.flexlease.order.repository;

import com.flexlease.order.domain.DisputeAiSuggestion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 纠纷仲裁建议缓存仓库（LLM 输出落库）。
 */
public interface DisputeAiSuggestionRepository extends JpaRepository<DisputeAiSuggestion, UUID> {

    Optional<DisputeAiSuggestion> findByDisputeId(UUID disputeId);
}
