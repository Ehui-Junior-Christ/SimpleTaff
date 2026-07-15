package com.siege.platform.poste;

import com.siege.platform.agent.AgentTerrain;
import com.siege.platform.agent.AgentTerrainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class AffectationService {

    private final AffectationRepository affectationRepository;
    private final PosteRepository posteRepository;
    private final AgentTerrainRepository agentTerrainRepository;

    public AffectationService(AffectationRepository affectationRepository, 
                              PosteRepository posteRepository, 
                              AgentTerrainRepository agentTerrainRepository) {
        this.affectationRepository = affectationRepository;
        this.posteRepository = posteRepository;
        this.agentTerrainRepository = agentTerrainRepository;
    }

    @Transactional
    public Affectation creerAffectation(UUID posteId, UUID agentId, LocalDate dateDebut) {
        // Vérifier la contrainte critique métier
        if (affectationRepository.existsByAgentIdAndStatut(agentId, "ACTIVE")) {
            throw new IllegalStateException("Cet agent possède déjà une affectation active.");
        }

        Poste poste = posteRepository.findById(posteId)
                .orElseThrow(() -> new IllegalArgumentException("Poste introuvable"));
        AgentTerrain agent = agentTerrainRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent introuvable"));

        Affectation affectation = new Affectation();
        affectation.setEntreprise(poste.getEntreprise());
        affectation.setPoste(poste);
        affectation.setAgent(agent);
        affectation.setDateDebutOccupation(dateDebut);
        affectation.setStatut("ACTIVE");

        poste.setStatut("POURVU");
        posteRepository.save(poste);

        return affectationRepository.save(affectation);
    }

    @Transactional
    public Affectation remplacerAgent(UUID affectationActuelleId, UUID nouvelAgentId, LocalDate dateRemplacement) {
        // 1. Clôturer l'affectation en cours
        Affectation ancienneAffectation = affectationRepository.findById(affectationActuelleId)
                .orElseThrow(() -> new IllegalArgumentException("Affectation introuvable"));

        if (!"ACTIVE".equals(ancienneAffectation.getStatut())) {
            throw new IllegalStateException("L'affectation n'est pas active.");
        }

        ancienneAffectation.setStatut("CLOTUREE");
        ancienneAffectation.setMotifFin("REMPLACEMENT");
        ancienneAffectation.setDateFinOccupation(dateRemplacement);
        affectationRepository.save(ancienneAffectation);

        // 2. Ouvrir la nouvelle affectation (atomique grâce à @Transactional)
        return creerAffectation(ancienneAffectation.getPoste().getId(), nouvelAgentId, dateRemplacement);
    }

    @Transactional
    public void cloturerAffectation(UUID affectationId, String motif, LocalDate dateFin) {
        Affectation affectation = affectationRepository.findById(affectationId)
                .orElseThrow(() -> new IllegalArgumentException("Affectation introuvable"));

        affectation.setStatut("CLOTUREE");
        affectation.setMotifFin(motif); // DEMISSION, URGENCE, etc.
        affectation.setDateFinOccupation(dateFin);
        affectationRepository.save(affectation);

        Poste poste = affectation.getPoste();
        poste.setStatut("OUVERT");
        posteRepository.save(poste);
    }
}
