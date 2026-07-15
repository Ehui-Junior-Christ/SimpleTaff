package com.siege.platform.coordonnateur;

import com.siege.platform.agent.AgentTerrain;
import com.siege.platform.agent.AgentTerrainRepository;
import com.siege.platform.poste.Affectation;
import com.siege.platform.poste.AffectationRepository;
import com.siege.platform.zone.Zone;
import com.siege.platform.zone.ZoneRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/coordonnateur")
@PreAuthorize("hasAnyRole('COORDONNATEUR', 'ADMIN_ENTREPRISE', 'SUPER_ADMIN')")
public class CoordonnateurController {

    private final AgentTerrainRepository agentRepo;
    private final AffectationRepository affectationRepo;
    private final ZoneRepository zoneRepo;

    public CoordonnateurController(AgentTerrainRepository agentRepo,
                                    AffectationRepository affectationRepo,
                                    ZoneRepository zoneRepo) {
        this.agentRepo = agentRepo;
        this.affectationRepo = affectationRepo;
        this.zoneRepo = zoneRepo;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAgents", agentRepo.count());
        stats.put("totalAffectations", affectationRepo.count());
        stats.put("pointagesAujourdhui", 0); // Sera enrichi avec la query pointage réelle
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/agents")
    public ResponseEntity<List<Map<String, Object>>> getAgents() {
        List<AgentTerrain> agents = agentRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (AgentTerrain a : agents) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("nom", a.getNom());
            map.put("prenom", a.getPrenom());
            map.put("contact", a.getContact());
            map.put("statut", a.getStatut());
            map.put("zoneNom", a.getZone() != null ? a.getZone().getNom() : null);
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/affectations")
    public ResponseEntity<List<Map<String, Object>>> getAffectations() {
        List<Affectation> affectations = affectationRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Affectation af : affectations) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", af.getId());
            map.put("statut", af.getStatut());
            map.put("dateDebut", af.getDateDebutOccupation() != null ? af.getDateDebutOccupation().toString() : null);
            map.put("agentNom", af.getAgent() != null
                    ? af.getAgent().getNom() + " " + af.getAgent().getPrenom() : null);
            map.put("posteLibelle", af.getPoste() != null && af.getPoste().getEmploi() != null
                    ? af.getPoste().getEmploi().getLibelle() : null);
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pointages/today")
    public ResponseEntity<List<Object>> getPointagesToday() {
        // Retourne une liste vide pour l'instant — à connecter au PointageRepository
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/zones")
    public ResponseEntity<List<Map<String, Object>>> getZones() {
        List<Zone> zones = zoneRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Zone z : zones) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", z.getId());
            map.put("nom", z.getNom());
            map.put("description", z.getDescription());
            map.put("perimetre", z.getPerimetre());
            map.put("statut", z.getStatut());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
}
