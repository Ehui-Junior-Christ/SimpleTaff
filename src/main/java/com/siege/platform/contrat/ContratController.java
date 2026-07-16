package com.siege.platform.contrat;

import com.siege.platform.agent.AgentTerrainRepository;
import com.siege.platform.common.CurrentTenantService;
import com.siege.platform.structuredemandeuse.StructureDemandeuseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/contrats")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'SUPER_ADMIN')")
public class ContratController {

    private final ContratAgentRepository contratRepository;
    private final RenouvellementContratRepository renouvellementRepository;
    private final AgentTerrainRepository agentRepository;
    private final StructureDemandeuseRepository structureRepository;
    private final CurrentTenantService tenantService;

    public ContratController(ContratAgentRepository contratRepository,
                             RenouvellementContratRepository renouvellementRepository,
                             AgentTerrainRepository agentRepository,
                             StructureDemandeuseRepository structureRepository,
                             CurrentTenantService tenantService) {
        this.contratRepository = contratRepository;
        this.renouvellementRepository = renouvellementRepository;
        this.agentRepository = agentRepository;
        this.structureRepository = structureRepository;
        this.tenantService = tenantService;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return contratRepository.findAll().stream().map(this::toMap).toList();
    }

    @GetMapping("/agent/{agentId}")
    public List<Map<String, Object>> byAgent(@PathVariable UUID agentId) {
        return contratRepository.findByAgentIdOrderByDateDebutDesc(agentId).stream().map(this::toMap).toList();
    }

    @GetMapping("/expirations")
    public List<Map<String, Object>> expirations(@RequestParam(defaultValue = "30") int jours) {
        return contratRepository.findByDateFinBetweenAndStatut(LocalDate.now(), LocalDate.now().plusDays(jours), "ACTIF")
                .stream().map(this::toMap).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> payload) {
        ContratAgent contrat = new ContratAgent();
        contrat.setEntreprise(tenantService.entreprise());
        contrat.setAgent(agentRepository.findById(UUID.fromString((String) payload.get("agentId"))).orElseThrow());
        contrat.setType((String) payload.get("type"));
        contrat.setDateDebut(LocalDate.parse((String) payload.get("dateDebut")));
        if (payload.get("dateFin") != null && !payload.get("dateFin").toString().isBlank()) {
            contrat.setDateFin(LocalDate.parse((String) payload.get("dateFin")));
        }
        if (payload.get("structureClienteId") != null && !payload.get("structureClienteId").toString().isBlank()) {
            contrat.setStructureCliente(structureRepository.findById(UUID.fromString((String) payload.get("structureClienteId"))).orElse(null));
        }
        contrat.setDirection((String) payload.get("direction"));
        contrat.setDocumentUrl((String) payload.get("documentUrl"));
        return ResponseEntity.ok(toMap(contratRepository.save(contrat)));
    }

    @PostMapping("/{id}/renouvellements")
    public ResponseEntity<?> renouveler(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        ContratAgent contrat = contratRepository.findById(id).orElseThrow();
        RenouvellementContrat renouvellement = new RenouvellementContrat();
        renouvellement.setContrat(contrat);
        renouvellement.setAncienneDateFin(contrat.getDateFin());
        renouvellement.setNouvelleDateFin(LocalDate.parse((String) payload.get("nouvelleDateFin")));
        renouvellement.setMotif((String) payload.get("motif"));
        renouvellement.setDocumentUrl((String) payload.get("documentUrl"));
        contrat.setDateFin(renouvellement.getNouvelleDateFin());
        renouvellementRepository.save(renouvellement);
        contratRepository.save(contrat);
        return ResponseEntity.ok(Map.of("message", "Contrat renouvele."));
    }

    private Map<String, Object> toMap(ContratAgent c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("agentId", c.getAgent() != null ? c.getAgent().getId() : null);
        m.put("agentNom", c.getAgent() != null ? c.getAgent().getNom() + " " + c.getAgent().getPrenom() : null);
        m.put("type", c.getType());
        m.put("dateDebut", c.getDateDebut());
        m.put("dateFin", c.getDateFin());
        m.put("structureCliente", c.getStructureCliente() != null ? c.getStructureCliente().getRaisonSociale() : null);
        m.put("direction", c.getDirection());
        m.put("statut", c.getStatut());
        return m;
    }
}
