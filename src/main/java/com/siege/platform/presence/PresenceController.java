package com.siege.platform.presence;

import com.siege.platform.pointage.Pointage;
import com.siege.platform.pointage.PointageRepository;
import com.siege.platform.rapport.RapportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/presences")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'COORDONNATEUR', 'EMPLOYEUR', 'SUPER_ADMIN')")
public class PresenceController {
    private final PointageRepository pointageRepository;
    private final RapportService rapportService;

    public PresenceController(PointageRepository pointageRepository, RapportService rapportService) {
        this.pointageRepository = pointageRepository;
        this.rapportService = rapportService;
    }

    @GetMapping
    public List<Map<String, Object>> mensuel(@RequestParam String mois) {
        YearMonth ym = YearMonth.parse(mois);
        List<Pointage> pointages = pointageRepository.findByDateHeureEntreeBetweenOrderByDateHeureEntreeDesc(
                ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay());
        return pointages.stream().map(this::ligne).toList();
    }

    @GetMapping("/export")
    public ResponseEntity<?> export(@RequestParam String mois,
                                    @RequestParam(defaultValue = "xlsx") String format) {
        Map<String, Object> rapport = rapportService.genererRapportPresences(mois);
        
        byte[] content = "pdf".equals(format) ? 
                rapportService.exportToPdf(rapport) : 
                rapportService.exportToExcel(rapport);

        String filename = "presences-" + mois + "." + format;
        MediaType mediaType = "pdf".equals(format) ? MediaType.APPLICATION_PDF : MediaType.TEXT_PLAIN;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content);
    }

    private Map<String, Object> ligne(Pointage p) {
        LocalDateTime entree = p.getDateHeureEntree();
        LocalDateTime sortie = p.getDateHeureSortie();
        long duree = sortie == null ? 0 : Duration.between(entree, sortie).toMinutes();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("agentNom", p.getAffectation().getAgent().getNom() + " " + p.getAffectation().getAgent().getPrenom());
        m.put("date", entree.toLocalDate());
        m.put("heureArrivee", entree.toLocalTime());
        m.put("heureDepart", sortie != null ? sortie.toLocalTime() : null);
        m.put("retard", entree.toLocalTime().isAfter(LocalTime.of(8, 0)));
        m.put("heuresSupplementaires", Math.max(0, (duree - 480) / 60.0));
        m.put("travailNuit", sortie != null && sortie.toLocalTime().isAfter(LocalTime.of(21, 0)));
        m.put("dimancheTravaille", entree.getDayOfWeek() == DayOfWeek.SUNDAY);
        m.put("jourFerie", false);
        return m;
    }
}
