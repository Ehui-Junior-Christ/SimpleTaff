package com.siege.platform.materiel;

import com.siege.platform.agent.AgentTerrain;
import com.siege.platform.agent.AgentTerrainRepository;
import com.siege.platform.common.CurrentTenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/materiels")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'COORDONNATEUR', 'SUPER_ADMIN')")
@Transactional
public class MaterielController {
    private final MaterielRepository materielRepository;
    private final AffectationMaterielRepository affectationRepository;
    private final AgentTerrainRepository agentRepository;
    private final CurrentTenantService tenantService;
    private final DemandeMaterielRepository demandeRepository;
    private final com.siege.platform.notification.NotificationService notificationService;

    public MaterielController(MaterielRepository materielRepository,
                              AffectationMaterielRepository affectationRepository,
                              AgentTerrainRepository agentRepository,
                              CurrentTenantService tenantService,
                              DemandeMaterielRepository demandeRepository,
                              com.siege.platform.notification.NotificationService notificationService) {
        this.materielRepository = materielRepository;
        this.affectationRepository = affectationRepository;
        this.agentRepository = agentRepository;
        this.tenantService = tenantService;
        this.demandeRepository = demandeRepository;
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Materiel> list(@RequestParam(required = false) String categorie) {
        return categorie == null ? materielRepository.findAll() : materielRepository.findByCategorieOrderByLibelle(categorie);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> payload) {
        Materiel materiel = new Materiel();
        materiel.setEntreprise(tenantService.entreprise());
        
        String libelle = (String) payload.get("libelle");
        if (libelle == null || libelle.isBlank()) {
            libelle = (String) payload.get("nom");
        }
        if (libelle == null || libelle.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le libellé du matériel est obligatoire."));
        }
        materiel.setLibelle(libelle);
        
        String categorie = (String) payload.get("categorie");
        if (categorie == null || categorie.isBlank()) {
            categorie = "AUTRE";
        }
        materiel.setCategorie(categorie);
        
        String statut = (String) payload.get("statut");
        if (statut == null || statut.isBlank()) {
            statut = "DISPONIBLE";
        }
        materiel.setStatut(statut);
        
        if (payload.get("valeurAchat") != null && !payload.get("valeurAchat").toString().isBlank()) {
            try {
                materiel.setValeurAchat(new java.math.BigDecimal(payload.get("valeurAchat").toString()));
            } catch (Exception e) {
                materiel.setValeurAchat(java.math.BigDecimal.ZERO);
            }
        } else if (payload.get("valeur") != null && !payload.get("valeur").toString().isBlank()) {
            try {
                materiel.setValeurAchat(new java.math.BigDecimal(payload.get("valeur").toString()));
            } catch (Exception e) {
                materiel.setValeurAchat(java.math.BigDecimal.ZERO);
            }
        } else {
            materiel.setValeurAchat(java.math.BigDecimal.ZERO);
        }
        
        if (payload.get("numeroSerie") != null) {
            materiel.setNumeroSerie((String) payload.get("numeroSerie"));
        } else if (payload.get("serie") != null) {
            materiel.setNumeroSerie((String) payload.get("serie"));
        }
        
        if (payload.get("description") != null) materiel.setDescription((String) payload.get("description"));
        if (payload.get("marque") != null) materiel.setMarque((String) payload.get("marque"));
        if (payload.get("modele") != null) materiel.setModele((String) payload.get("modele"));
        
        return ResponseEntity.ok(materielRepository.save(materiel));
    }

    @GetMapping("/demandes")
    public List<DemandeMateriel> listDemandes() {
        return demandeRepository.findByOrderByDateDemandeDesc();
    }

    @PostMapping("/demandes")
    public ResponseEntity<?> createDemande(@RequestBody Map<String, Object> payload) {
        DemandeMateriel demande = new DemandeMateriel();
        demande.setEntreprise(tenantService.entreprise());
        demande.setLibelle((String) payload.get("libelle"));
        
        String categorie = (String) payload.get("categorie");
        demande.setCategorie(categorie != null && !categorie.isBlank() ? categorie : "EQUIPEMENT");
        demande.setMotif((String) payload.get("motif"));

        if (payload.get("numeroSerie") != null) {
            demande.setNumeroSerie((String) payload.get("numeroSerie"));
        }
        
        if (payload.get("valeurAchat") != null && !payload.get("valeurAchat").toString().isBlank()) {
            try {
                demande.setValeurAchat(new java.math.BigDecimal(payload.get("valeurAchat").toString()));
            } catch (Exception e) {
                demande.setValeurAchat(java.math.BigDecimal.ZERO);
            }
        }

        String coordEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        demande.setCoordonnateurNom(coordEmail);

        DemandeMateriel saved = demandeRepository.save(demande);

        notificationService.creerAlerte(
            saved.getEntreprise(),
            "DEMANDE_MATERIEL",
            "Nouvelle demande de matériel: '" + saved.getLibelle() + "' (Valeur: " + saved.getValeurAchat() + " EUR, N° Série: " + (saved.getNumeroSerie() != null ? saved.getNumeroSerie() : "N/A") + ")"
        );

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/demandes/{id}/traiter")
    public ResponseEntity<?> traiterDemande(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        DemandeMateriel demande = demandeRepository.findById(id).orElseThrow();
        String nouveauStatut = payload.get("statut");
        demande.setStatut(nouveauStatut);

        DemandeMateriel saved = demandeRepository.save(demande);

        // If approved, automatically insert the new material into inventory
        if ("APPROUVEE".equalsIgnoreCase(nouveauStatut) || "ACCORDE".equalsIgnoreCase(nouveauStatut)) {
            Materiel mat = new Materiel();
            mat.setEntreprise(saved.getEntreprise());
            mat.setLibelle(saved.getLibelle());
            mat.setCategorie(saved.getCategorie());
            mat.setNumeroSerie(saved.getNumeroSerie());
            mat.setValeurAchat(saved.getValeurAchat() != null ? saved.getValeurAchat() : java.math.BigDecimal.ZERO);
            mat.setStatut("DISPONIBLE");
            mat.setDescription("Créé suite à l'approbation de la demande de matériel");
            materielRepository.save(mat);
        }

        notificationService.creerAlerte(
            saved.getEntreprise(),
            "DEMANDE_MATERIEL_DECISION",
            "La demande de matériel '" + saved.getLibelle() + "' a été " + nouveauStatut.toLowerCase()
        );

        return ResponseEntity.ok(saved);
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
        materiel.setStatut("ASSIGNE");
        materielRepository.save(materiel);
        return ResponseEntity.ok(affectationRepository.save(affectation));
    }

    @PostMapping("/{id}/retour")
    public ResponseEntity<?> retour(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        Materiel materiel = materielRepository.findById(id).orElseThrow();
        Optional<AffectationMateriel> affOpt = affectationRepository.findFirstByMaterielIdAndStatut(id, "REMIS");
        if (affOpt.isPresent()) {
            AffectationMateriel affectation = affOpt.get();
            affectation.setDateRetour(LocalDateTime.now());
            affectation.setSignatureRetourUrl((String) payload.get("signatureUrl"));
            affectation.setStatut("RETOURNE");
            affectationRepository.save(affectation);
        }
        materiel.setStatut("DISPONIBLE");
        materielRepository.save(materiel);
        return ResponseEntity.ok(materiel);
    }

    @PostMapping("/{id}/incident")
    public ResponseEntity<?> incident(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        Materiel materiel = materielRepository.findById(id).orElseThrow();
        String nouveauStatut = payload.getOrDefault("statut", "DEFECTUEUX");
        String details = payload.getOrDefault("details", "Incident ou altération signalé");
        
        // Allowed incident statuses: DEFECTUEUX, INUTILISABLE, PERDU, REPARATION
        materiel.setStatut(nouveauStatut.toUpperCase());
        if (details != null && !details.isBlank()) {
            materiel.setDescription((materiel.getDescription() != null ? materiel.getDescription() + " | " : "") + details);
        }

        // If material is PERDU or INUTILISABLE, close active affectation if present
        Optional<AffectationMateriel> affOpt = affectationRepository.findFirstByMaterielIdAndStatut(id, "REMIS");
        if (affOpt.isPresent() && ("PERDU".equalsIgnoreCase(nouveauStatut) || "INUTILISABLE".equalsIgnoreCase(nouveauStatut))) {
            AffectationMateriel aff = affOpt.get();
            aff.setDateRetour(LocalDateTime.now());
            aff.setStatut("INCIDENT_" + nouveauStatut.toUpperCase());
            affectationRepository.save(aff);
        }

        Materiel saved = materielRepository.save(materiel);

        notificationService.creerAlerte(
            saved.getEntreprise(),
            "INCIDENT_MATERIEL",
            "Alerte Matériel: Équipement '" + saved.getLibelle() + "' déclaré comme " + nouveauStatut.toUpperCase() + ". Motif: " + details
        );

        return ResponseEntity.ok(saved);
    }
}
