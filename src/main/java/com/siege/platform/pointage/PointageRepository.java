package com.siege.platform.pointage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PointageRepository extends JpaRepository<Pointage, UUID> {
    @EntityGraph(attributePaths = {"affectation", "affectation.agent"})
    Optional<Pointage> findByAffectationIdAndDateHeureSortieIsNull(UUID affectationId);

    @EntityGraph(attributePaths = {"affectation", "affectation.agent"})
    Optional<Pointage> findFirstByAffectationIdAndDateHeureEntreeBetweenOrderByDateHeureEntreeDesc(
            UUID affectationId,
            LocalDateTime start,
            LocalDateTime end
    );

    @EntityGraph(attributePaths = {"affectation", "affectation.agent"})
    List<Pointage> findByDateHeureEntreeBetweenOrderByDateHeureEntreeDesc(LocalDateTime start, LocalDateTime end);
}
