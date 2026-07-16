package com.siege.platform.conge;

import com.siege.platform.agent.AgentTerrainRepository;
import com.siege.platform.common.CurrentTenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/conges")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'COORDONNATEUR', 'SUPER_ADMIN')")
public class CongeController {
    private final DemandeCongeRepository demandeRepository;
    private final SoldeCongeRepository soldeRepository;
    private final AgentTerrainRepository agentRepository;
    private final CurrentTenantService tenantService;

    public CongeController(DemandeCongeRepository demandeRepository,
                           SoldeCongeRepository soldeRepository,
                           AgentTerrainRepository agentRepository,
                           CurrentTenantService tenantService) {
        this.demandeRepository = demandeRepository;
        this.soldeRepository = soldeRepository;
        this.agentRepository = agentRepository;
        this.tenantService = tenantService;
    }

    @GetMapping
    public List<DemandeConge> list(@RequestParam(required = false) UUID agentId) {
        return agentId == null ? demandeRepository.findAll() : demandeRepository.findByAgentIdOrderByDateDebutDesc(agentId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> payload) {
        DemandeConge demande = new DemandeConge();
        demande.setEntreprise(tenantService.entreprise());
        demande.setAgent(agentRepository.findById(UUID.fromString((String) payload.get("agentId"))).orElseThrow());
        demande.setType((String) payload.get("type"));
        demande.setDateDebut(LocalDate.parse((String) payload.get("dateDebut")));
        demande.setDateFin(LocalDate.parse((String) payload.get("dateFin")));
        demande.setMotif((String) payload.get("motif"));
        demande.setJustificatifUrl((String) payload.get("justificatifUrl"));
        if (!"ABSENCE_INJUSTIFIEE".equals(demande.getType())) {
            demande.setStatut("EN_ATTENTE_RH");
        }
        return ResponseEntity.ok(demandeRepository.save(demande));
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<?> valider(@PathVariable UUID id, @RequestParam String etape) {
        DemandeConge demande = demandeRepository.findById(id).orElseThrow();
        if ("SUPERVISEUR".equalsIgnoreCase(etape)) demande.setStatut("EN_ATTENTE_RH");
        else if ("RH".equalsIgnoreCase(etape)) demande.setStatut("EN_ATTENTE_DIRECTION");
        else if ("DIRECTION".equalsIgnoreCase(etape)) demande.setStatut("VALIDEE");
        return ResponseEntity.ok(demandeRepository.save(demande));
    }

    @GetMapping("/solde/{agentId}")
    public Map<String, Object> solde(@PathVariable UUID agentId, @RequestParam(defaultValue = "2026") Integer annee) {
        SoldeConge solde = soldeRepository.findByAgentIdAndAnnee(agentId, annee).orElseGet(() -> {
            SoldeConge s = new SoldeConge();
            s.setEntreprise(tenantService.entreprise());
            s.setAgent(agentRepository.findById(agentId).orElseThrow());
            s.setAnnee(annee);
            s.setSoldeTotal(30);
            s.setJoursRestants(30);
            return soldeRepository.save(s);
        });
        return Map.of("soldeTotal", solde.getSoldeTotal(), "joursConsommes", solde.getJoursConsommes(), "joursRestants", solde.getJoursRestants());
    }

    static int jours(DemandeConge demande) {
        return (int) ChronoUnit.DAYS.between(demande.getDateDebut(), demande.getDateFin()) + 1;
    }
}
