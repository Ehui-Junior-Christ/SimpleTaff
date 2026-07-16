package com.siege.platform.contrat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "renouvellement_contrat")
@Getter
@Setter
public class RenouvellementContrat {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrat_id", nullable = false)
    private ContratAgent contrat;

    private LocalDate ancienneDateFin;

    @Column(nullable = false)
    private LocalDate nouvelleDateFin;

    private String motif;

    @Column(length = 500)
    private String documentUrl;

    @Column(nullable = false)
    private LocalDateTime creeLe = LocalDateTime.now();
}
