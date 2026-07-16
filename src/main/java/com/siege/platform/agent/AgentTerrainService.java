package com.siege.platform.agent;

import com.siege.platform.pointage.CarteAgent;
import com.siege.platform.pointage.CarteAgentRepository;
import com.siege.platform.utilisateur.Utilisateur;
import com.siege.platform.utilisateur.UtilisateurRepository;
import com.siege.platform.zone.Zone;
import com.siege.platform.zone.ZoneRepository;
import com.siege.platform.common.QRCodeUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AgentTerrainService {

    private final AgentTerrainRepository agentTerrainRepository;
    private final CarteAgentRepository carteAgentRepository;
    private final ZoneRepository zoneRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final QRCodeUtil qrCodeUtil;

    public AgentTerrainService(AgentTerrainRepository agentTerrainRepository,
                               CarteAgentRepository carteAgentRepository,
                               ZoneRepository zoneRepository,
                               UtilisateurRepository utilisateurRepository,
                               QRCodeUtil qrCodeUtil) {
        this.agentTerrainRepository = agentTerrainRepository;
        this.carteAgentRepository = carteAgentRepository;
        this.zoneRepository = zoneRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.qrCodeUtil = qrCodeUtil;
    }

    /**
     * Liste uniquement les agents de l'entreprise du compte connecté (multi-tenant).
     */
    public List<AgentTerrain> listAll() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non connecté."));
        if (user.getEntreprise() == null) {
            throw new IllegalArgumentException("Aucune entreprise associée à ce compte.");
        }
        return agentTerrainRepository.findByEntrepriseId(user.getEntreprise().getId());
    }

    @Transactional
    public AgentTerrain creerAgent(String nom, String prenom, String contact, UUID zoneId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non connecté."));

        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("Zone introuvable."));

        AgentTerrain agent = new AgentTerrain();
        agent.setEntreprise(user.getEntreprise());
        agent.setZone(zone);
        agent.setNom(nom);
        agent.setPrenom(prenom);
        agent.setContact(contact);
        agent.setStatut("ACTIF");

        AgentTerrain savedAgent = agentTerrainRepository.save(agent);

        // Create agent QR card (signed QR)
        CarteAgent carte = new CarteAgent();
        carte.setAgent(savedAgent);

        String agentFullName = savedAgent.getNom() + " " + savedAgent.getPrenom();
        String signedQRCode = qrCodeUtil.generateQRCode(savedAgent.getId(), agentFullName);

        carte.setCodeQr(signedQRCode);
        carte.setStatut("ACTIVE");
        carteAgentRepository.save(carte);

        return savedAgent;
    }


    @Transactional
    public AgentTerrain creerAgentDepuisPayload(java.util.Map<String, String> payload) {
        AgentTerrain agent = creerAgent(
                payload.getOrDefault("nom", "").trim(),
                payload.getOrDefault("prenom", "").trim(),
                payload.getOrDefault("contact", "").trim(),
                UUID.fromString(payload.getOrDefault("zoneId", "").trim())
        );
        agent.setTelephoneSecondaire(payload.get("telephoneSecondaire"));
        agent.setSituationMatrimoniale(payload.get("situationMatrimoniale"));
        agent.setNombreEnfants(parseInt(payload.get("nombreEnfants")));
        agent.setContactUrgenceNom(payload.get("contactUrgenceNom"));
        agent.setContactUrgenceTelephone(payload.get("contactUrgenceTelephone"));
        agent.setContactUrgenceLien(payload.get("contactUrgenceLien"));
        return agentTerrainRepository.save(agent);
    }

    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Integer.parseInt(value);
    }
}
