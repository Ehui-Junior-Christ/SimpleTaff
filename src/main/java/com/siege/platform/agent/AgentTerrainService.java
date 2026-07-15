package com.siege.platform.agent;

import com.siege.platform.pointage.CarteAgent;
import com.siege.platform.pointage.CarteAgentRepository;
import com.siege.platform.utilisateur.Utilisateur;
import com.siege.platform.utilisateur.UtilisateurRepository;
import com.siege.platform.zone.Zone;
import com.siege.platform.zone.ZoneRepository;
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

    public AgentTerrainService(AgentTerrainRepository agentTerrainRepository,
                               CarteAgentRepository carteAgentRepository,
                               ZoneRepository zoneRepository,
                               UtilisateurRepository utilisateurRepository) {
        this.agentTerrainRepository = agentTerrainRepository;
        this.carteAgentRepository = carteAgentRepository;
        this.zoneRepository = zoneRepository;
        this.utilisateurRepository = utilisateurRepository;
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
        // Get logged user's Enterprise to link the agent to the tenant
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

        // Automatically generate the QR code card for the agent
        CarteAgent carte = new CarteAgent();
        carte.setAgent(savedAgent);
        carte.setCodeQr(savedAgent.getId().toString());
        carte.setStatut("ACTIVE");
        carteAgentRepository.save(carte);

        return savedAgent;
    }
}
