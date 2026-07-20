package com.siege.platform.contrat;

import com.siege.platform.agent.AgentTerrainRepository;
import com.siege.platform.common.CurrentTenantService;
import com.siege.platform.structuredemandeuse.StructureDemandeuseRepository;
import com.siege.platform.audit.AuditLog;
import com.siege.platform.audit.AuditLogRepository;
import com.siege.platform.notification.NotificationService;
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
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    public ContratController(ContratAgentRepository contratRepository,
                             RenouvellementContratRepository renouvellementRepository,
                             AgentTerrainRepository agentRepository,
                             StructureDemandeuseRepository structureRepository,
                             CurrentTenantService tenantService,
                             AuditLogRepository auditLogRepository,
                             NotificationService notificationService) {
        this.contratRepository = contratRepository;
        this.renouvellementRepository = renouvellementRepository;
        this.agentRepository = agentRepository;
        this.structureRepository = structureRepository;
        this.tenantService = tenantService;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
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
        contrat.setStatut("ACTIF");

        // Map new fields
        if (payload.get("salaireBase") != null) {
            contrat.setSalaireBase(new java.math.BigDecimal(payload.get("salaireBase").toString()));
        }
        contrat.setFonction((String) payload.get("fonction"));
        contrat.setDepartement((String) payload.get("departement"));

        ContratAgent saved = contratRepository.save(contrat);

        // Audit log
        AuditLog audit = new AuditLog();
        audit.setEntreprise(saved.getEntreprise());
        audit.setUtilisateurEmail(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());
        audit.setAction("CREATION_CONTRAT");
        audit.setModule("RH_CONTRAT");
        audit.setCibleId(saved.getId().toString());
        audit.setDetails("Création du contrat de type " + saved.getType() + " pour l'agent : " + saved.getAgent().getNom() + " " + saved.getAgent().getPrenom());
        auditLogRepository.save(audit);

        // Notification
        notificationService.creerAlerte(saved.getEntreprise(), "RH_CONTRAT", "Nouveau contrat de type " + saved.getType() + " créé pour l'agent " + saved.getAgent().getNom() + " " + saved.getAgent().getPrenom());

        return ResponseEntity.ok(toMap(saved));
    }

    @PostMapping("/{id}/renouvellements")
    public ResponseEntity<?> renouveler(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        ContratAgent contrat = contratRepository.findById(id).orElseThrow();
        
        // Validation d'éligibilité : CDD renouvelable max 2 fois
        int renewalsCount = renouvellementRepository.findByContratIdOrderByCreeLeDesc(id).size();
        if ("CDD".equalsIgnoreCase(contrat.getType()) && renewalsCount >= 2) {
            return ResponseEntity.badRequest().body(Map.of("message", "Conformément à la réglementation locale, un contrat CDD ne peut pas être renouvelé plus de 2 fois."));
        }

        RenouvellementContrat renouvellement = new RenouvellementContrat();
        renouvellement.setContrat(contrat);
        renouvellement.setAncienneDateFin(contrat.getDateFin());
        renouvellement.setNouvelleDateFin(LocalDate.parse((String) payload.get("nouvelleDateFin")));
        renouvellement.setMotif((String) payload.get("motif"));
        renouvellement.setDocumentUrl((String) payload.get("documentUrl"));
        
        contrat.setDateFin(renouvellement.getNouvelleDateFin());
        
        renouvellementRepository.save(renouvellement);
        contratRepository.save(contrat);

        // Audit log
        AuditLog audit = new AuditLog();
        audit.setEntreprise(contrat.getEntreprise());
        audit.setUtilisateurEmail(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());
        audit.setAction("RENOUVELLEMENT_CONTRAT");
        audit.setModule("RH_CONTRAT");
        audit.setCibleId(contrat.getId().toString());
        audit.setDetails("Renouvellement de contrat pour l'agent : " + contrat.getAgent().getNom() + " " + contrat.getAgent().getPrenom() + ". Nouvelle date fin: " + renouvellement.getNouvelleDateFin());
        auditLogRepository.save(audit);

        // Notification
        notificationService.creerAlerte(contrat.getEntreprise(), "RH_CONTRAT", "Contrat de l'agent " + contrat.getAgent().getNom() + " " + contrat.getAgent().getPrenom() + " renouvelé jusqu'au " + renouvellement.getNouvelleDateFin());

        return ResponseEntity.ok(Map.of("message", "Contrat renouvele."));
    }

    @GetMapping("/{id}/renouvellements")
    public ResponseEntity<List<RenouvellementContrat>> getRenouvellements(@PathVariable UUID id) {
        return ResponseEntity.ok(renouvellementRepository.findByContratIdOrderByCreeLeDesc(id));
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
        m.put("salaireBase", c.getSalaireBase());
        m.put("fonction", c.getFonction());
        m.put("departement", c.getDepartement());
        return m;
    }
}
