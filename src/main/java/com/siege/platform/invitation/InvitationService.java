package com.siege.platform.invitation;

import com.siege.platform.common.enums.FormuleAbonnement;
import com.siege.platform.common.enums.Role;
import com.siege.platform.common.enums.StatutUtilisateur;
import com.siege.platform.entreprise.Entreprise;
import com.siege.platform.entreprise.EntrepriseRepository;
import com.siege.platform.utilisateur.AdminEntreprise;
import com.siege.platform.utilisateur.UtilisateurRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InvitationService {

    private final InvitationEntrepriseRepository invitationRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public InvitationService(
            InvitationEntrepriseRepository invitationRepository,
            EntrepriseRepository entrepriseRepository,
            UtilisateurRepository utilisateurRepository,
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder) {
        this.invitationRepository = invitationRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Crée une entreprise (si non existante), génère un token unique et envoie le lien par email.
     */
    @Transactional
    public InvitationEntreprise creerEtEnvoyerInvitation(String nomEntreprise, String formuleStr,
                                                          double tauxCotisation, String emailDestinataire) {
        // Créer ou retrouver l'entreprise
        Entreprise entreprise = new Entreprise();
        entreprise.setNom(nomEntreprise);
        try {
            entreprise.setFormuleAbonnement(FormuleAbonnement.valueOf(formuleStr));
        } catch (IllegalArgumentException e) {
            entreprise.setFormuleAbonnement(FormuleAbonnement.PRO);
        }
        entreprise.setTauxCotisation(new java.math.BigDecimal(String.valueOf(tauxCotisation)));
        entreprise.setStatut(com.siege.platform.common.enums.StatutEntreprise.INACTIF); // inactif jusqu'à inscription
        Entreprise savedEntreprise = entrepriseRepository.save(entreprise);

        // Générer un token unique
        String token = UUID.randomUUID().toString().replace("-", "");

        InvitationEntreprise invitation = new InvitationEntreprise();
        invitation.setToken(token);
        invitation.setEmailDestinataire(emailDestinataire);
        invitation.setEntreprise(savedEntreprise);
        invitation.setFormuleAbonnement(savedEntreprise.getFormuleAbonnement());
        InvitationEntreprise savedInvitation = invitationRepository.save(invitation);

        // Envoyer l'email
        String lien = "http://localhost:8080/vitrine/inscription.html?token=" + token;
        envoyerEmailInvitation(emailDestinataire, nomEntreprise, lien, savedEntreprise.getFormuleAbonnement().name());

        return savedInvitation;
    }

    /**
     * Valide un token : vérifie qu'il existe, n'est pas utilisé et n'est pas expiré.
     */
    public InvitationEntreprise validerToken(String token) {
        InvitationEntreprise invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Lien d'invitation invalide ou introuvable."));

        if (invitation.isUtilise()) {
            throw new RuntimeException("Ce lien d'invitation a déjà été utilisé.");
        }
        if (LocalDateTime.now().isAfter(invitation.getDateExpiration())) {
            throw new RuntimeException("Ce lien d'invitation a expiré (validité 30 minutes).");
        }
        return invitation;
    }

    /**
     * Finalise l'inscription de l'admin entreprise via le token d'invitation.
     */
    @Transactional
    public void inscrireAdminEntreprise(String token, String nom, String prenom, String password) {
        InvitationEntreprise invitation = validerToken(token);

        // Créer l'utilisateur AdminEntreprise
        AdminEntreprise admin = new AdminEntreprise();
        admin.setNom(nom);
        admin.setPrenom(prenom);
        admin.setEmail(invitation.getEmailDestinataire());
        admin.setMotDePasseHash(passwordEncoder.encode(password));
        admin.setStatut(StatutUtilisateur.ACTIF);
        admin.setEntreprise(invitation.getEntreprise());
        utilisateurRepository.save(admin);

        // Activer l'entreprise
        Entreprise entreprise = invitation.getEntreprise();
        entreprise.setStatut(com.siege.platform.common.enums.StatutEntreprise.ACTIF);
        entrepriseRepository.save(entreprise);

        // Marquer le token comme utilisé
        invitation.setUtilise(true);
        invitationRepository.save(invitation);
    }

    private void envoyerEmailInvitation(String destinataire, String nomEntreprise, String lien, String formule) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("SimpleTaff <juniorehui15@gmail.com>");
            helper.setTo(destinataire);
            helper.setSubject("Activez votre espace SimpleTaff - " + nomEntreprise);
            
            String htmlContent = "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "  <meta charset='utf-8'>"
                + "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "  <style>"
                + "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; color: #1e293b; margin: 0; padding: 0; }"
                + "    .container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); border: 1px solid #e2e8f0; }"
                + "    .header { background: linear-gradient(135deg, #4f46e5, #0284c7); padding: 32px; text-align: center; }"
                + "    .header h1 { color: #ffffff; margin: 0; font-size: 24px; font-weight: 800; }"
                + "    .content { padding: 40px 32px; line-height: 1.6; font-size: 15px; }"
                + "    .welcome { font-size: 18px; font-weight: 700; color: #0f172a; margin-top: 0; margin-bottom: 16px; }"
                + "    .badge-container { margin: 24px 0; padding: 16px; background-color: #f1f5f9; border-radius: 12px; border: 1px solid #e2e8f0; }"
                + "    .cta-container { text-align: center; margin: 32px 0 24px; }"
                + "    .cta-button { display: inline-block; background: linear-gradient(135deg, #4f46e5, #0284c7); color: #ffffff !important; text-decoration: none; padding: 14px 32px; font-size: 15px; font-weight: 700; border-radius: 12px; box-shadow: 0 10px 15px -3px rgba(79, 70, 229, 0.3); }"
                + "    .warning { font-size: 13px; color: #b45309; background-color: #fef3c7; border: 1px solid #fde68a; padding: 14px; border-radius: 10px; margin-top: 24px; }"
                + "    .footer { background-color: #f8fafc; padding: 24px 32px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #f1f5f9; }"
                + "  </style>"
                + "</head>"
                + "<body>"
                + "  <div class='container'>"
                + "    <div class='header'>"
                + "      <h1>SimpleTaff</h1>"
                + "    </div>"
                + "    <div class='content'>"
                + "      <p class='welcome'>Bonjour,</p>"
                + "      <p>Le Super Administrateur de <strong>SimpleTaff</strong> vous invite à activer l'espace de votre entreprise pour commencer la gestion simplifiée de votre main-d'œuvre.</p>"
                + "      <div class='badge-container'>"
                + "        <table style='width:100%; border-collapse:collapse;'>"
                + "          <tr>"
                + "            <td style='padding:4px 0; color:#64748b; font-weight:500;'>Entreprise :</td>"
                + "            <td style='padding:4px 0; color:#0f172a; font-weight:700; text-align:right;'>" + nomEntreprise + "</td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='padding:4px 0; color:#64748b; font-weight:500;'>Formule d'abonnement :</td>"
                + "            <td style='padding:4px 0; color:#0f172a; font-weight:700; text-align:right;'><span style='background-color:#4f46e5; color:#ffffff; padding:2px 8px; border-radius:100px; font-size:12px;'>" + formule + "</span></td>"
                + "          </tr>"
                + "        </table>"
                + "      </div>"
                + "      <p>Pour configurer votre compte administrateur et activer votre espace, cliquez sur le bouton ci-dessous :</p>"
                + "      <div class='cta-container'>"
                + "        <a href='" + lien + "' class='cta-button'>Activer mon espace entreprise</a>"
                + "      </div>"
                + "      <div class='warning'>"
                + "        <strong>Note importante :</strong> Ce lien d'activation est à usage unique et expire dans 30 minutes."
                + "      </div>"
                + "    </div>"
                + "    <div class='footer'>"
                + "      <p>Cet e-mail est automatique, merci de ne pas y répondre.</p>"
                + "      <p>© 2026 SimpleTaff. Tous droits réservés.</p>"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("[InvitationService] Erreur envoi email HTML: " + e.getMessage());
        }
    }
}
