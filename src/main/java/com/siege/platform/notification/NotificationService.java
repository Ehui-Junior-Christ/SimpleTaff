package com.siege.platform.notification;

import com.siege.platform.entreprise.Entreprise;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationService {
    private final NotificationEvenementRepository repository;
    private final JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public NotificationService(NotificationEvenementRepository repository, JavaMailSender mailSender) {
        this.repository = repository;
        this.mailSender = mailSender;
    }

    /**
     * Create an alert and store in database
     */
    public void creerAlerte(Entreprise entreprise, String type, String message) {
        NotificationEvenement event = new NotificationEvenement();
        event.setEntreprise(entreprise);
        event.setType(type);
        event.setMessage(message);
        repository.save(event);
    }

    /**
     * Send email notification to an enterprise contact
     */
    public void envoyerEmailNotification(String destinataire, String titre, String contenu) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinataire);
            message.setSubject(titre);
            message.setText(contenu);
            message.setFrom("simpletaff@platform.com");
            mailSender.send(message);
            logger.info("Email envoyé à {}: {}", destinataire, titre);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi d'email à {}: {}", destinataire, e.getMessage());
        }
    }

    /**
     * Send alert and email notification
     */
    public void creerAlerteAvecEmail(Entreprise entreprise, String type, String message, String emailDestinaire) {
        creerAlerte(entreprise, type, message);
        envoyerEmailNotification(emailDestinaire, "Notification - " + type, message);
    }

    /**
     * Send hourly activity alerts
     */
    public void envoyerAlerteHeureRonde(String emailAdmin, String agentName, int heure) {
        String titre = "Alerte pointage - Heure ronde";
        String contenu = "L'agent " + agentName + " a scanné sa carte à " + String.format("%02d:00", heure) + ".";
        envoyerEmailNotification(emailAdmin, titre, contenu);
    }

    /**
     * Send confirmation when agent completes their shift
     */
    public void envoyerConfirmationFinEquipe(String emailAgent, String agentName, long minutesDuree) {
        String titre = "Confirmation de fin d'équipe";
        String heures = minutesDuree / 60;
        String minutes = minutesDuree % 60;
        String contenu = String.format(
                "Bonjour %s,\n\nVotre équipe de %d heures et %d minutes a été enregistrée.\n\nCordialement,\nSimpleTaff",
                agentName, heures, minutes
        );
        envoyerEmailNotification(emailAgent, titre, contenu);
    }

    /**
     * Send late arrival notification
     */
    public void envoyerAlerteRetard(String emailAdmin, String agentName, int minutesRetard) {
        String titre = "Alerte retard";
        String contenu = "L'agent " + agentName + " est arrivé avec " + minutesRetard + " minutes de retard.";
        envoyerEmailNotification(emailAdmin, titre, contenu);
    }
}
