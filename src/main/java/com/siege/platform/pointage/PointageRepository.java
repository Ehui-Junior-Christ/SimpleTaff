package com.siege.platform.pointage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
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

    @EntityGraph(attributePaths = {"affectation", "affectation.agent", "affectation.poste", "affectation.poste.site"})
    List<Pointage> findByDateHeureEntreeBetweenOrderByDateHeureEntreeDesc(LocalDateTime start, LocalDateTime end);

    long countByDateHeureEntreeBetween(LocalDateTime start, LocalDateTime end);

    @Query(value = """
            SELECT CAST(date_heure_entree AS date) AS jour, COUNT(*) AS total
            FROM pointage
            GROUP BY CAST(date_heure_entree AS date)
            ORDER BY jour DESC
            """, nativeQuery = true)
    List<Object[]> findPointageDatesWithCounts();
}
