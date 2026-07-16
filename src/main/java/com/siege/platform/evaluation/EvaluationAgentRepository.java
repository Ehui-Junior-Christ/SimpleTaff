package com.siege.platform.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationAgentRepository extends JpaRepository<EvaluationAgent, UUID> {
    List<EvaluationAgent> findByAgentIdOrderByAnneeDesc(UUID agentId);
}
