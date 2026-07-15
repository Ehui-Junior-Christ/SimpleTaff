package com.siege.platform.facturation;

import com.siege.platform.entreprise.Entreprise;
import com.siege.platform.poste.Affectation;
import com.siege.platform.structuredemandeuse.StructureDemandeuse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "facture")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "entreprise_id = :entrepriseId")
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_demandeuse_id", nullable = false)
    private StructureDemandeuse structureDemandeuse;

    @Column(nullable = false)
    private String periode;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montantFacture;

    @Column(nullable = false, length = 500)
    private String rapportPointageUrl;

    @Column(nullable = false)
    private String statutPaiement;

    @Column(nullable = false)
    private LocalDateTime dateEmission;

    @Column(nullable = false)
    private String modePaiement = "VIREMENT_BANCAIRE";

    @ManyToMany
    @JoinTable(
            name = "facture_affectation",
            joinColumns = @JoinColumn(name = "facture_id"),
            inverseJoinColumns = @JoinColumn(name = "affectation_id")
    )
    private Set<Affectation> affectations = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        dateEmission = LocalDateTime.now();
    }
}
