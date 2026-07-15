package com.siege.platform.agent;

import com.siege.platform.pointage.CarteAgent;
import com.siege.platform.pointage.CarteAgentRepository;
import com.siege.platform.zone.Zone;
import com.siege.platform.zone.ZoneRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/agents")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'COORDONNATEUR', 'SUPER_ADMIN')")
public class AgentTerrainController {

    private final AgentTerrainService agentTerrainService;
    private final ZoneRepository zoneRepository;
    private final CarteAgentRepository carteAgentRepository;

    public AgentTerrainController(AgentTerrainService agentTerrainService,
                                  ZoneRepository zoneRepository,
                                  CarteAgentRepository carteAgentRepository) {
        this.agentTerrainService = agentTerrainService;
        this.zoneRepository = zoneRepository;
        this.carteAgentRepository = carteAgentRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAgents() {
        List<AgentTerrain> agents = agentTerrainService.listAll();
        List<Map<String, Object>> response = new ArrayList<>();
        
        // Load all active cards to map them easily
        List<CarteAgent> cartes = carteAgentRepository.findAll();
        Map<UUID, String> agentQrMap = new HashMap<>();
        for (CarteAgent c : cartes) {
            if ("ACTIVE".equals(c.getStatut())) {
                agentQrMap.put(c.getAgent().getId(), c.getCodeQr());
            }
        }

        for (AgentTerrain a : agents) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("nom", a.getNom());
            map.put("prenom", a.getPrenom());
            map.put("contact", a.getContact());
            map.put("statut", a.getStatut());
            map.put("zoneNom", a.getZone() != null ? a.getZone().getNom() : "—");
            map.put("codeQr", agentQrMap.getOrDefault(a.getId(), a.getId().toString()));
            response.add(map);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> ajouterAgent(@RequestBody Map<String, String> payload) {
        String nom = payload.getOrDefault("nom", "").trim();
        String prenom = payload.getOrDefault("prenom", "").trim();
        String contact = payload.getOrDefault("contact", "").trim();
        String zoneIdStr = payload.getOrDefault("zoneId", "").trim();

        if (nom.isEmpty() || prenom.isEmpty() || contact.isEmpty() || zoneIdStr.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tous les champs sont requis."));
        }

        try {
            UUID zoneId = UUID.fromString(zoneIdStr);
            AgentTerrain agent = agentTerrainService.creerAgent(nom, prenom, contact, zoneId);
            return ResponseEntity.ok(Map.of(
                "message", "Agent créé avec succès !",
                "agentId", agent.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/zones")
    public ResponseEntity<List<Zone>> getZones() {
        return ResponseEntity.ok(zoneRepository.findAll());
    }
}
