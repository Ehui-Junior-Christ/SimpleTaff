package com.siege.platform.pointage;

import com.siege.platform.poste.Affectation;
import com.siege.platform.poste.Poste;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pointages")
public class PointageController {

    private final PointageService pointageService;
    private final PointageRepository pointageRepository;

    public PointageController(PointageService pointageService, PointageRepository pointageRepository) {
        this.pointageService = pointageService;
        this.pointageRepository = pointageRepository;
    }

    @PostMapping("/scanner")
    @PreAuthorize("hasAnyRole('COORDONNATEUR', 'EMPLOYEUR', 'ADMIN_ENTREPRISE')")
    public ResponseEntity<?> scannerPointage(@RequestBody Map<String, String> payload) {
        String cardId = payload.get("cardId");
        if (cardId == null || cardId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "L'identifiant de la carte est requis."));
        }
        
        try {
            Pointage pointage = pointageService.scannerCarte(cardId, payload.get("typePointage"));
            return ResponseEntity.ok(toResponse(pointage));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", safeMessage(e)));
        }
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('COORDONNATEUR', 'EMPLOYEUR', 'ADMIN_ENTREPRISE')")
    public ResponseEntity<List<Map<String, Object>>> getPointagesToday() {
        return getPointagesByDate(LocalDate.now());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDONNATEUR', 'EMPLOYEUR', 'ADMIN_ENTREPRISE')")
    public ResponseEntity<List<Map<String, Object>>> getPointages(@RequestParam(required = false) LocalDate date) {
        return getPointagesByDate(date != null ? date : LocalDate.now());
    }

    @GetMapping("/dates")
    @PreAuthorize("hasAnyRole('COORDONNATEUR', 'EMPLOYEUR', 'ADMIN_ENTREPRISE')")
    public ResponseEntity<List<Map<String, Object>>> getPointageDates() {
        return ResponseEntity.ok(toDateSummary(pointageRepository.findPointageDatesWithCounts()));
    }

    private ResponseEntity<List<Map<String, Object>>> getPointagesByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<Map<String, Object>> response = new ArrayList<>();

        for (Pointage pointage : pointageRepository.findByDateHeureEntreeBetweenOrderByDateHeureEntreeDesc(start, end)) {
            response.add(toResponse(pointage));
        }
        return ResponseEntity.ok(response);
    }

    private List<Map<String, Object>> toDateSummary(List<Object[]> rows) {
        List<Map<String, Object>> response = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("date", row[0] != null ? row[0].toString() : null);
            map.put("total", row[1]);
            response.add(map);
        }
        return response;
    }

    private Map<String, Object> toResponse(Pointage pointage) {
        Map<String, Object> map = new LinkedHashMap<>();
        Affectation affectation = pointage.getAffectation();
        Poste poste = affectation != null ? affectation.getPoste() : null;

        map.put("id", pointage.getId());
        map.put("agentNom", resolveAgentNom(affectation));
        map.put("typePointage", pointage.getDateHeureSortie() == null ? "ENTREE" : "SORTIE");
        map.put("dateHeure", pointage.getDateHeureSortie() != null ? pointage.getDateHeureSortie() : pointage.getDateHeureEntree());
        map.put("dateHeureEntree", pointage.getDateHeureEntree());
        map.put("dateHeureSortie", pointage.getDateHeureSortie());
        map.put("siteNom", poste != null && poste.getSite() != null ? poste.getSite().getNom() : null);
        map.put("statut", pointage.getStatut());
        return map;
    }

    private String resolveAgentNom(Affectation affectation) {
        if (affectation == null || affectation.getAgent() == null) {
            return null;
        }
        return affectation.getAgent().getNom() + " " + affectation.getAgent().getPrenom();
    }

    private String safeMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Erreur lors de la validation du pointage.";
    }
}
