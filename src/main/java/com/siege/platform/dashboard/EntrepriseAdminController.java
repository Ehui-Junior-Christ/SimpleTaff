package com.siege.platform.dashboard;

import com.siege.platform.emploi.Emploi;
import com.siege.platform.emploi.EmploiRepository;
import com.siege.platform.poste.*;
import com.siege.platform.structuredemandeuse.Site;
import com.siege.platform.structuredemandeuse.SiteRepository;
import com.siege.platform.entreprise.Entreprise;
import com.siege.platform.entreprise.EntrepriseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'SUPER_ADMIN')")
public class EntrepriseAdminController {

    private final SiteRepository siteRepository;
    private final PosteRepository posteRepository;
    private final AffectationRepository affectationRepository;
    private final AffectationService affectationService;
    private final EmploiRepository emploiRepository;
    private final EntrepriseRepository entrepriseRepository;

    public EntrepriseAdminController(SiteRepository siteRepository,
                                     PosteRepository posteRepository,
                                     AffectationRepository affectationRepository,
                                     AffectationService affectationService,
                                     EmploiRepository emploiRepository,
                                     EntrepriseRepository entrepriseRepository) {
        this.siteRepository = siteRepository;
        this.posteRepository = posteRepository;
        this.affectationRepository = affectationRepository;
        this.affectationService = affectationService;
        this.emploiRepository = emploiRepository;
        this.entrepriseRepository = entrepriseRepository;
    }


    @GetMapping("/sites")
    public ResponseEntity<List<Map<String, Object>>> getSites() {
        List<Site> sites = siteRepository.findAll();
        List<Map<String, Object>> response = new ArrayList<>();
        for (Site s : sites) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("nom", s.getNom());
            map.put("adresse", s.getAdresse());
            map.put("zoneNom", s.getZone() != null ? s.getZone().getNom() : "—");
            response.add(map);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/emplois")
    public ResponseEntity<List<Emploi>> getEmplois() {
        return ResponseEntity.ok(emploiRepository.findAll());
    }

    @GetMapping("/postes")
    public ResponseEntity<List<Map<String, Object>>> getPostes() {
        List<Poste> postes = posteRepository.findAll();
        List<Map<String, Object>> response = new ArrayList<>();
        for (Poste p : postes) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("siteNom", p.getSite() != null ? p.getSite().getNom() : "—");
            map.put("emploiLibelle", p.getEmploi() != null ? p.getEmploi().getLibelle() : "—");
            map.put("salaireBrut", p.getSalaireBrutNegocie());
            map.put("statut", p.getStatut());
            response.add(map);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/affectations")
    public ResponseEntity<List<Map<String, Object>>> getAffectations() {
        List<Affectation> affectations = affectationRepository.findAll();
        List<Map<String, Object>> response = new ArrayList<>();
        for (Affectation a : affectations) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("agentNom", a.getAgent() != null ? a.getAgent().getNom() + " " + a.getAgent().getPrenom() : "—");
            map.put("posteLibelle", a.getPoste() != null && a.getPoste().getEmploi() != null ? a.getPoste().getEmploi().getLibelle() : "—");
            map.put("siteNom", a.getPoste() != null && a.getPoste().getSite() != null ? a.getPoste().getSite().getNom() : "—");
            map.put("dateDebut", a.getDateDebutOccupation() != null ? a.getDateDebutOccupation().toString() : "—");
            map.put("dateFin", a.getDateFinOccupation() != null ? a.getDateFinOccupation().toString() : "—");
            map.put("statut", a.getStatut());
            response.add(map);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/affectations")
    public ResponseEntity<?> affecterAgent(@RequestBody Map<String, String> payload) {
        String posteIdStr = payload.get("posteId");
        String agentIdStr = payload.get("agentId");
        String dateStr = payload.getOrDefault("dateDebut", LocalDate.now().toString());

        if (posteIdStr == null || agentIdStr == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Poste et Agent requis."));
        }

        try {
            UUID posteId = UUID.fromString(posteIdStr);
            UUID agentId = UUID.fromString(agentIdStr);
            LocalDate dateDebut = LocalDate.parse(dateStr);
            Affectation aff = affectationService.creerAffectation(posteId, agentId, dateDebut);
            return ResponseEntity.ok(Map.of("message", "Affectation créée !", "id", aff.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/affectations/{id}/cloturer")
    public ResponseEntity<?> cloturerAffectation(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        String motif = payload.getOrDefault("motif", "FIN_CONTRAT");
        String dateStr = payload.getOrDefault("dateFin", LocalDate.now().toString());

        try {
            LocalDate dateFin = LocalDate.parse(dateStr);
            affectationService.cloturerAffectation(id, motif, dateFin);
            return ResponseEntity.ok(Map.of("message", "Affectation clôturée."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/entreprise/config")
    public ResponseEntity<?> getEntrepriseConfig() {
        UUID enterpriseId = com.siege.platform.config.tenant.TenantContext.getTenantId();
        if (enterpriseId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Aucune entreprise dans le contexte."));
        }
        Entreprise e = entrepriseRepository.findById(enterpriseId).orElse(null);
        if (e == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nom", e.getNom());
        map.put("statut", e.getStatut());
        map.put("formuleAbonnement", e.getFormuleAbonnement());
        map.put("tauxCotisation", e.getTauxCotisation());
        map.put("seuilAbsenceLongueJours", e.getSeuilAbsenceLongueJours());
        map.put("tauxRetenueReduite", e.getTauxRetenueReduite());
        return ResponseEntity.ok(map);
    }

    @PutMapping("/entreprise/config")
    public ResponseEntity<?> updateEntrepriseConfig(@RequestBody Map<String, Object> payload) {
        UUID enterpriseId = com.siege.platform.config.tenant.TenantContext.getTenantId();
        if (enterpriseId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Aucune entreprise dans le contexte."));
        }
        Entreprise e = entrepriseRepository.findById(enterpriseId).orElse(null);
        if (e == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            if (payload.containsKey("tauxCotisation")) {
                e.setTauxCotisation(new BigDecimal(payload.get("tauxCotisation").toString()));
            }
            if (payload.containsKey("seuilAbsenceLongueJours")) {
                e.setSeuilAbsenceLongueJours(((Number) payload.get("seuilAbsenceLongueJours")).intValue());
            }
            if (payload.containsKey("tauxRetenueReduite")) {
                e.setTauxRetenueReduite(new BigDecimal(payload.get("tauxRetenueReduite").toString()));
            }
            entrepriseRepository.save(e);
            return ResponseEntity.ok(Map.of("message", "Configuration mise à jour avec succès !"));
        } catch (Exception err) {
            return ResponseEntity.badRequest().body(Map.of("message", err.getMessage()));
        }
    }

    @PostMapping("/postes")
    public ResponseEntity<?> createPoste(@RequestBody Map<String, Object> payload) {
        UUID enterpriseId = com.siege.platform.config.tenant.TenantContext.getTenantId();
        if (enterpriseId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Aucune entreprise dans le contexte."));
        }
        try {
            UUID siteId = UUID.fromString((String) payload.get("siteId"));
            UUID empleoId = UUID.fromString((String) payload.get("emploiId"));
            BigDecimal salaireBrutNegocie = new BigDecimal(payload.get("salaireBrutNegocie").toString());
            BigDecimal montantRetenueForfaitaire = new BigDecimal(payload.get("montantRetenueForfaitaire").toString());

            Site site = siteRepository.findById(siteId).orElseThrow(() -> new RuntimeException("Site introuvable."));
            Emploi emploi = emploiRepository.findById(empleoId).orElseThrow(() -> new RuntimeException("Emploi introuvable."));
            Entreprise entreprise = entrepriseRepository.findById(enterpriseId).orElseThrow(() -> new RuntimeException("Entreprise introuvable."));

            Poste poste = new Poste();
            poste.setEntreprise(entreprise);
            poste.setSite(site);
            poste.setEmploi(emploi);
            poste.setSalaireBrutNegocie(salaireBrutNegocie);
            poste.setMontantRetenueForfaitaire(montantRetenueForfaitaire);
            poste.setStatut("OUVERT");

            Poste saved = posteRepository.save(poste);
            return ResponseEntity.ok(Map.of("message", "Poste créé avec succès !", "id", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/postes/{id}")
    public ResponseEntity<?> deletePoste(@PathVariable UUID id) {
        if (!posteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        posteRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Poste supprimé."));
    }
}

