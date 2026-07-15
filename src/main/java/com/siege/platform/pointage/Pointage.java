package com.siege.platform.pointage;

import com.siege.platform.entreprise.Entreprise;
import com.siege.platform.poste.Affectation;
import com.siege.platform.utilisateur.Employeur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pointage")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "entreprise_id = :entrepriseId")
public class Pointage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affectation_id", nullable = false)
    private Affectation affectation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carte_scannee_id", nullable = false)
    private CarteAgent carteScannee;

    @Column(nullable = false)
    private LocalDateTime dateHeureEntree;

    private LocalDateTime dateHeureSortie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_employeur_id")
    private Employeur valideParEmployeur;

    private LocalDateTime dateValidation;

    @Column(nullable = false)
    private String statut;
}
