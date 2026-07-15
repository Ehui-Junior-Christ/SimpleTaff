package com.siege.platform.paie;

import com.siege.platform.agent.AgentTerrain;
import com.siege.platform.entreprise.Entreprise;
import com.siege.platform.poste.Affectation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulletin_de_paie")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "entreprise_id = :entrepriseId")
public class BulletinDePaie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentTerrain agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affectation_id", nullable = false)
    private Affectation affectation;

    @Column(nullable = false)
    private String periode;

    @Column(nullable = false)
    private Integer joursPrevus;

    @Column(nullable = false)
    private Integer joursValides;

    @Column(nullable = false)
    private Integer joursAbsenceJustifieeCourte = 0;

    @Column(nullable = false)
    private Integer joursAbsenceJustifieeLongue = 0;

    @Column(nullable = false)
    private Integer joursAbsenceNonJustifiee = 0;

    @Column(nullable = false)
    private Integer joursCongePaye = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal salaireBrutEffectif;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal salaireNetCalcule;

    @Column(nullable = false)
    private LocalDateTime dateCloture;

    @Column(nullable = false)
    private String statutPaiement;
}
