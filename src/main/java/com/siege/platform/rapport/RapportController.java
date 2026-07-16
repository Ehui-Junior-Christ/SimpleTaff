package com.siege.platform.rapport;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rapports")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'SUPER_ADMIN')")
public class RapportController {

    private final RapportService rapportService;

    public RapportController(RapportService rapportService) {
        this.rapportService = rapportService;
    }

    @GetMapping("/{type}")
    public ResponseEntity<Map<String, Object>> getRapport(@PathVariable String type,
                                                          @RequestParam String mois) {
        Map<String, Object> rapport = switch (type) {
            case "pointages" -> rapportService.genererRapportPointages(mois, "json");
            case "presences" -> rapportService.genererRapportPresences(mois);
            case "facturation" -> rapportService.genererRapportFacturation(mois);
            default -> throw new IllegalArgumentException("Type de rapport invalide: " + type);
        };
        return ResponseEntity.ok(rapport);
    }

    @GetMapping("/{type}/export")
    public ResponseEntity<?> export(@PathVariable String type,
                                    @RequestParam(defaultValue = "pdf") String format,
                                    @RequestParam String mois) {
        Map<String, Object> rapport = switch (type) {
            case "pointages" -> rapportService.genererRapportPointages(mois, format);
            case "presences" -> rapportService.genererRapportPresences(mois);
            case "facturation" -> rapportService.genererRapportFacturation(mois);
            default -> throw new IllegalArgumentException("Type de rapport invalide: " + type);
        };

        byte[] content = "pdf".equals(format) ? 
                rapportService.exportToPdf(rapport) : 
                rapportService.exportToExcel(rapport);

        String filename = type + "-" + mois + "." + format;
        MediaType mediaType = "pdf".equals(format) ? MediaType.APPLICATION_PDF : MediaType.TEXT_PLAIN;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content);
    }
}
