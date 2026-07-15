package com.siege.platform.dashboard;

import com.siege.platform.agent.AgentTerrainRepository;
import com.siege.platform.facturation.FactureRepository;
import com.siege.platform.poste.AffectationRepository;
import com.siege.platform.poste.PosteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AgentTerrainRepository agentTerrainRepository;
    private final PosteRepository posteRepository;
    private final AffectationRepository affectationRepository;
    private final FactureRepository factureRepository;

    public DashboardController(AgentTerrainRepository agentTerrainRepository,
                               PosteRepository posteRepository,
                               AffectationRepository affectationRepository,
                               FactureRepository factureRepository) {
        this.agentTerrainRepository = agentTerrainRepository;
        this.posteRepository = posteRepository;
        this.affectationRepository = affectationRepository;
        this.factureRepository = factureRepository;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN_ENTREPRISE')")
    public ResponseEntity<Map<String, Object>> getAdminDashboardData() {
        Map<String, Object> stats = new HashMap<>();
        // Les repositories filtrent déjà automatiquement par l'ID de l'entreprise via l'Aspect
        stats.put("totalAgents", agentTerrainRepository.count());
        stats.put("totalPostes", posteRepository.count());
        stats.put("totalAffectationsActives", affectationRepository.count()); // Simplifié pour la démo
        
        return ResponseEntity.ok(stats);
    }
}
