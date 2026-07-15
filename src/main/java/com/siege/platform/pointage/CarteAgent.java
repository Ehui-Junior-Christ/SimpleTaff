package com.siege.platform.pointage;

import com.siege.platform.agent.AgentTerrain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "carte_agent")
@Getter
@Setter
public class CarteAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentTerrain agent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String codeQr;

    @Column(nullable = false)
    private String statut;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateEmission;

    @PrePersist
    protected void onCreate() {
        dateEmission = LocalDateTime.now();
    }
}
