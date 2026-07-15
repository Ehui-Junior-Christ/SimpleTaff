package com.siege.platform.poste;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AffectationRepository extends JpaRepository<Affectation, UUID> {
    
    @EntityGraph(attributePaths = {"agent"})
    Optional<Affectation> findByAgentIdAndStatut(UUID agentId, String statut);
    
    boolean existsByAgentIdAndStatut(UUID agentId, String statut);
}
