package com.siege.platform.rapport;

import com.siege.platform.pointage.Pointage;
import com.siege.platform.pointage.PointageRepository;
import com.siege.platform.utilisateur.Utilisateur;
import com.siege.platform.utilisateur.UtilisateurRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for generating various reports in PDF and Excel formats
 */
@Service
public class RapportService {

    private final PointageRepository pointageRepository;

    public RapportService(PointageRepository pointageRepository) {
        this.pointageRepository = pointageRepository;
    }


    /**
     * Generate a comprehensive pointage report
     */
    public Map<String, Object> genererRapportPointages(String mois, String format) {
        YearMonth ym = YearMonth.parse(mois);
        LocalDateTime debut = ym.atDay(1).atStartOfDay();
        LocalDateTime fin = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<Pointage> pointages = pointageRepository.findByDateHeureEntreeBetweenOrderByDateHeureEntreeDesc(debut, fin);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("titre", "Rapport de Pointages - " + mois);
        report.put("dateGeneration", LocalDate.now());
        report.put("nombre_entrees", pointages.size());
        report.put("duree_totale_minutes", calculateTotalDuration(pointages));

        // Group by agent
        Map<String, List<Map<String, Object>>> parAgent = new LinkedHashMap<>();
        for (Pointage p : pointages) {
            // Affectation -> agent
            String agentKey = p.getAffectation().getAgent().getId().toString();
            parAgent.computeIfAbsent(agentKey, k -> new ArrayList<>())
                    .add(pointageToMap(p));
        }

        report.put("par_agent", parAgent);
        report.put("total_agents", parAgent.size());

        return report;
    }

    /**
     * Generate an attendance/presence report
     */
    public Map<String, Object> genererRapportPresences(String mois) {
        YearMonth ym = YearMonth.parse(mois);
        LocalDateTime debut = ym.atDay(1).atStartOfDay();
        LocalDateTime fin = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<Pointage> pointages = pointageRepository.findByDateHeureEntreeBetweenOrderByDateHeureEntreeDesc(debut, fin);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("titre", "Rapport de Présences - " + mois);
        report.put("dateGeneration", LocalDate.now());
        report.put("period", mois);

        // Calculate presence statistics
        long journeesPresentes = pointages.stream()
                .map(p -> p.getDateHeureEntree().toLocalDate())
                .distinct()
                .count();

        Map<String, Integer> retards = new LinkedHashMap<>();
        pointages.stream()
                .filter(p -> p.getDateHeureEntree().toLocalTime().isAfter(java.time.LocalTime.of(8, 0)))
                .forEach(p -> {
                    String agent = p.getAffectation().getAgent().getNom();
                    retards.merge(agent, 1, Integer::sum);
                });

        report.put("journees_presentes", journeesPresentes);
        report.put("retards_par_agent", retards);
        report.put("total_retards", retards.values().stream().mapToInt(Integer::intValue).sum());
        report.put("lignes", pointages.stream().map(this::pointageToMap).toList());

        return report;
    }

    /**
     * Generate a facturation/billing report
     */
    public Map<String, Object> genererRapportFacturation(String mois) {
        YearMonth ym = YearMonth.parse(mois);
        LocalDateTime debut = ym.atDay(1).atStartOfDay();
        LocalDateTime fin = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<Pointage> pointages = pointageRepository.findByDateHeureEntreeBetweenOrderByDateHeureEntreeDesc(debut, fin);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("titre", "Rapport de Facturation - " + mois);
        report.put("dateGeneration", LocalDate.now());
        report.put("period", mois);

        long totalHeures = calculateTotalDuration(pointages) / 60;
        report.put("total_heures", totalHeures);
        report.put("taux_horaire_defaut", 15.0); // Default rate, should come from contract
        report.put("montant_total_estime", totalHeures * 15.0);

        // Group by affectation for invoice lines
        Map<String, Map<String, Object>> parAffectation = new LinkedHashMap<>();
        for (Pointage p : pointages) {
            String affectationKey = p.getAffectation().getId().toString();
            if (!parAffectation.containsKey(affectationKey)) {
                parAffectation.put(affectationKey, new LinkedHashMap<>());
                parAffectation.get(affectationKey).put("agent", p.getAffectation().getAgent().getNom());
                parAffectation.get(affectationKey).put("heures", 0L);
                parAffectation.get(affectationKey).put("pointages", new ArrayList<>());
            }

            long heures = calculateDurationMinutes(p) / 60;
            long currentHeures = (Long) parAffectation.get(affectationKey).get("heures");
            parAffectation.get(affectationKey).put("heures", currentHeures + heures);
            ((List<?>) parAffectation.get(affectationKey).get("pointages")).add(pointageToMap(p));
        }

        report.put("lignes_facturation", parAffectation);

        return report;
    }

    /**
     * Export report to Excel format (returns file path or content)
     */
    public byte[] exportToExcel(Map<String, Object> report) {
        // For now, return a simple CSV-like format
        StringBuilder csv = new StringBuilder();
        csv.append("Titre,").append(report.get("titre")).append("\n");
        csv.append("Date Génération,").append(report.get("dateGeneration")).append("\n");

        if (report.containsKey("lignes")) {
            csv.append("\nDETAILS:\n");
            ((List<?>) report.get("lignes")).forEach(item -> {
                if (item instanceof Map) {
                    csv.append(String.join(",", ((Map<?, ?>) item).values().stream()
                            .map(Object::toString).toArray(String[]::new))).append("\n");
                }
            });
        }

        return csv.toString().getBytes();
    }

    /**
     * Export report to PDF format (requires PDF library - placeholder)
     */
    public byte[] exportToPdf(Map<String, Object> report) {
        // Placeholder - implement with iText or Apache PDFBox
        String content = "PDF Export Placeholder\n" + report.toString();
        return content.getBytes();
    }

    // Helper methods

    private Map<String, Object> pointageToMap(Pointage p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("date", p.getDateHeureEntree().toLocalDate());
        map.put("heure_entree", p.getDateHeureEntree().toLocalTime());
        map.put("heure_sortie", p.getDateHeureSortie() != null ? p.getDateHeureSortie().toLocalTime() : "En cours");
        map.put("agent", p.getAffectation().getAgent().getNom() + " " + p.getAffectation().getAgent().getPrenom());
        map.put("duree_minutes", calculateDurationMinutes(p));
        return map;
    }

    private long calculateDurationMinutes(Pointage p) {
        if (p.getDateHeureSortie() == null) {
            return 0;
        }
        return java.time.Duration.between(p.getDateHeureEntree(), p.getDateHeureSortie()).toMinutes();
    }

    private long calculateTotalDuration(List<Pointage> pointages) {
        return pointages.stream()
                .mapToLong(this::calculateDurationMinutes)
                .sum();
    }
}
