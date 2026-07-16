package com.siege.platform.agent;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "piece_justificative")
@Getter
@Setter
public class PieceJustificative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentTerrain agent;

    @Column(nullable = false)
    private String type;

    private LocalDate dateEmission;

    private LocalDate dateExpiration;

    @Column(nullable = false)
    private String statut = "VALIDE";

    private LocalDate alerteEnvoyeeLe;

    @Column(nullable = false, length = 500)
    private String urlDocument;
}
