package com.siege.platform.poste;

import com.siege.platform.agent.AgentTerrain;
import com.siege.platform.entreprise.Entreprise;
import com.siege.platform.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "affectation")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "entreprise_id = :entrepriseId")
public class Affectation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id", nullable = false)
    private Poste poste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentTerrain agent;

    @Column(nullable = false)
    private LocalDate dateDebutOccupation;

    private LocalDate dateFinOccupation;

    private String commune;

    private String zoneOperationnelle;

    private String motifAffectation;

    @Column(length = 500)
    private String decisionUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_validant_id")
    private Utilisateur responsableValidant;

    private String motifFin;

    @Column(nullable = false)
    private String statut;
}
