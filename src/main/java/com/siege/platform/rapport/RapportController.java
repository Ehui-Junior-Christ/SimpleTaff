package com.siege.platform.rapport;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@RestController
@RequestMapping("/api/rapports")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'COORDONNATEUR', 'SUPER_ADMIN')")
@Transactional(readOnly = true)
public class RapportController {

    private final RapportService rapportService;

    public RapportController(RapportService rapportService) {
        this.rapportService = rapportService;
    }

    @GetMapping("/{type}")
    public ResponseEntity<Map<String, Object>> getRapport(@PathVariable String type,
                                                          @RequestParam(required = false) String mois) {
        if (mois == null || mois.isBlank()) {
            java.time.LocalDate now = java.time.LocalDate.now();
            mois = String.format("%d-%02d", now.getYear(), now.getMonthValue());
        }
        
        Map<String, Object> rapport = switch (type.toLowerCase()) {
            case "global", "synthese", "complet" -> rapportService.genererRapportGlobal(mois);
            case "pointages" -> rapportService.genererRapportPointages(mois, "json");
            case "presences" -> rapportService.genererRapportPresences(mois);
            case "conges" -> rapportService.genererRapportConges(mois);
            case "materiels" -> rapportService.genererRapportMateriels();
            case "disciplinaire" -> rapportService.genererRapportDisciplinaire(mois);
            case "missions" -> rapportService.genererRapportMissions(mois);
            case "facturation" -> rapportService.genererRapportFacturation(mois);
            default -> rapportService.genererRapportGlobal(mois);
        };
        return ResponseEntity.ok(rapport);
    }

    @GetMapping("/{type}/export")
    public ResponseEntity<?> export(@PathVariable String type,
                                    @RequestParam(defaultValue = "pdf") String format,
                                    @RequestParam(required = false) String mois) {
        if (mois == null || mois.isBlank()) {
            java.time.LocalDate now = java.time.LocalDate.now();
            mois = String.format("%d-%02d", now.getYear(), now.getMonthValue());
        }

        Map<String, Object> rapport = switch (type.toLowerCase()) {
            case "global", "synthese", "complet" -> rapportService.genererRapportGlobal(mois);
            case "pointages" -> rapportService.genererRapportPointages(mois, format);
            case "presences" -> rapportService.genererRapportPresences(mois);
            case "conges" -> rapportService.genererRapportConges(mois);
            case "materiels" -> rapportService.genererRapportMateriels();
            case "disciplinaire" -> rapportService.genererRapportDisciplinaire(mois);
            case "missions" -> rapportService.genererRapportMissions(mois);
            case "facturation" -> rapportService.genererRapportFacturation(mois);
            default -> rapportService.genererRapportGlobal(mois);
        };

        boolean isPdf = "pdf".equalsIgnoreCase(format);
        byte[] content = isPdf ? 
                rapportService.exportToPdf(rapport) : 
                rapportService.exportToExcel(rapport);

        String fileExtension = isPdf ? "pdf" : "csv";
        String filename = type + "-" + mois + "." + fileExtension;
        MediaType mediaType = isPdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("text/csv;charset=UTF-8");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content);
    }
}
