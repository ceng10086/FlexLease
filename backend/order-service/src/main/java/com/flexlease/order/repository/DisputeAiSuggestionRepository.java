package com.flexlease.order.repository;

import com.flexlease.order.domain.DisputeAiSuggestion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeAiSuggestionRepository extends JpaRepository<DisputeAiSuggestion, UUID> {

    Optional<DisputeAiSuggestion> findByDisputeId(UUID disputeId);
}

