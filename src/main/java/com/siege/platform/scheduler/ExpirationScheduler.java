package com.siege.platform.scheduler;

import com.siege.platform.agent.PieceJustificative;
import com.siege.platform.agent.PieceJustificativeRepository;
import com.siege.platform.contrat.ContratAgent;
import com.siege.platform.contrat.ContratAgentRepository;
import com.siege.platform.notification.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ExpirationScheduler {
    private final PieceJustificativeRepository pieceRepository;
    private final ContratAgentRepository contratRepository;
    private final NotificationService notificationService;

    public ExpirationScheduler(PieceJustificativeRepository pieceRepository,
                               ContratAgentRepository contratRepository,
                               NotificationService notificationService) {
        this.pieceRepository = pieceRepository;
        this.contratRepository = contratRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 7 * * *")
    public void scannerExpirations() {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(30);
        for (PieceJustificative piece : pieceRepository.findByDateExpirationBetween(today, limit)) {
            piece.setStatut("A_EXPIRER");
            piece.setAlerteEnvoyeeLe(today);
            pieceRepository.save(piece);
            notificationService.creerAlerte(piece.getAgent().getEntreprise(), "DOCUMENT_EXPIRATION", "Document a renouveler: " + piece.getType());
        }
        for (ContratAgent contrat : contratRepository.findByDateFinBetweenAndStatut(today, limit, "ACTIF")) {
            notificationService.creerAlerte(contrat.getEntreprise(), "CONTRAT_EXPIRATION", "Contrat a renouveler: " + contrat.getType());
        }
    }
}
