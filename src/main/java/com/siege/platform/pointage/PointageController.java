package com.siege.platform.pointage;

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
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Pointage> pointages = pointageRepository.findByDateHeureEntreeBetweenOrderByDateHeureEntreeDesc(start, end);
        List<Map<String, Object>> response = new ArrayList<>();
        for (Pointage pointage : pointages) {
            response.add(toResponse(pointage));
        }
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toResponse(Pointage pointage) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", pointage.getId());
        map.put("agentNom", pointage.getAffectation().getAgent().getNom() + " " + pointage.getAffectation().getAgent().getPrenom());
        map.put("typePointage", pointage.getDateHeureSortie() == null ? "ENTREE" : "SORTIE");
        map.put("dateHeure", pointage.getDateHeureSortie() != null ? pointage.getDateHeureSortie() : pointage.getDateHeureEntree());
        map.put("dateHeureEntree", pointage.getDateHeureEntree());
        map.put("dateHeureSortie", pointage.getDateHeureSortie());
        map.put("statut", pointage.getStatut());
        return map;
    }

    private String safeMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Erreur lors de la validation du pointage.";
    }
}
