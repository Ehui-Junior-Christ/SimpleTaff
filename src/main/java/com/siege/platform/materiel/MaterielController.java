package com.siege.platform.materiel;

import com.siege.platform.agent.AgentTerrainRepository;
import com.siege.platform.common.CurrentTenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/materiels")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'COORDONNATEUR', 'SUPER_ADMIN')")
public class MaterielController {
    private final MaterielRepository materielRepository;
    private final AffectationMaterielRepository affectationRepository;
    private final AgentTerrainRepository agentRepository;
    private final CurrentTenantService tenantService;

    public MaterielController(MaterielRepository materielRepository,
                              AffectationMaterielRepository affectationRepository,
                              AgentTerrainRepository agentRepository,
                              CurrentTenantService tenantService) {
        this.materielRepository = materielRepository;
        this.affectationRepository = affectationRepository;
        this.agentRepository = agentRepository;
        this.tenantService = tenantService;
    }

    @GetMapping
    public List<Materiel> list(@RequestParam(required = false) String categorie) {
        return categorie == null ? materielRepository.findAll() : materielRepository.findByCategorieOrderByLibelle(categorie);
    }

    @PostMapping
    public Materiel create(@RequestBody Materiel materiel) {
        materiel.setEntreprise(tenantService.entreprise());
        return materielRepository.save(materiel);
    }

    @GetMapping("/agent/{agentId}")
    public List<AffectationMateriel> byAgent(@PathVariable UUID agentId) {
        return affectationRepository.findByAgentIdOrderByDateRemiseDesc(agentId);
    }

    @PostMapping("/{id}/remise")
    public ResponseEntity<?> remise(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        Materiel materiel = materielRepository.findById(id).orElseThrow();
        AffectationMateriel affectation = new AffectationMateriel();
        affectation.setMateriel(materiel);
        affectation.setAgent(agentRepository.findById(UUID.fromString((String) payload.get("agentId"))).orElseThrow());
        affectation.setSignatureRemiseUrl((String) payload.get("signatureUrl"));
        materiel.setStatut("AFFECTE");
        materielRepository.save(materiel);
        return ResponseEntity.ok(affectationRepository.save(affectation));
    }

    @PostMapping("/{id}/retour")
    public ResponseEntity<?> retour(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        Materiel materiel = materielRepository.findById(id).orElseThrow();
        AffectationMateriel affectation = affectationRepository.findFirstByMaterielIdAndStatut(id, "REMIS").orElseThrow();
        affectation.setDateRetour(LocalDateTime.now());
        affectation.setSignatureRetourUrl((String) payload.get("signatureUrl"));
        affectation.setStatut("RETOURNE");
        materiel.setStatut("DISPONIBLE");
        materielRepository.save(materiel);
        return ResponseEntity.ok(affectationRepository.save(affectation));
    }

    @PostMapping("/{id}/incident")
    public ResponseEntity<?> incident(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        Materiel materiel = materielRepository.findById(id).orElseThrow();
        materiel.setStatut(payload.getOrDefault("statut", "CASSE"));
        return ResponseEntity.ok(materielRepository.save(materiel));
    }
}
