package com.siege.platform.dotation;

import com.siege.platform.pointage.CarteAgent;
import com.siege.platform.pointage.CarteAgentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DotationService {

    private final DemandeDotationRepository demandeDotationRepository;
    private final CarteAgentRepository carteAgentRepository;

    public DotationService(DemandeDotationRepository demandeDotationRepository, CarteAgentRepository carteAgentRepository) {
        this.demandeDotationRepository = demandeDotationRepository;
        this.carteAgentRepository = carteAgentRepository;
    }

    @Transactional
    public DemandeDotation mettreAJourStatut(UUID demandeId, String nouveauStatut) {
        DemandeDotation demande = demandeDotationRepository.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        demande.setStatut(nouveauStatut);

        if ("REMISE".equals(nouveauStatut)) {
            demande.setDateRemise(LocalDateTime.now());
            
            // Règle métier : Activation automatique si c'est une carte QR
            if ("CARTE_QR".equals(demande.getType())) {
                // S'assurer qu'il n'y a pas déjà de carte active, ou bien la désactiver (selon la logique, on la désactive pour faire place)
                carteAgentRepository.findAll().stream()
                        .filter(c -> c.getAgent().getId().equals(demande.getAgent().getId()) && "ACTIVE".equals(c.getStatut()))
                        .forEach(c -> {
                            c.setStatut("REMPLACEE");
                            carteAgentRepository.save(c);
                        });

                // Création et activation de la nouvelle carte
                CarteAgent nouvelleCarte = new CarteAgent();
                nouvelleCarte.setAgent(demande.getAgent());
                // TODO: Générer un vrai QR Code signé cryptographiquement (HMAC / JWT)
                nouvelleCarte.setCodeQr("QR_CODE_" + UUID.randomUUID().toString());
                nouvelleCarte.setStatut("ACTIVE");
                carteAgentRepository.save(nouvelleCarte);
            }
        }

        return demandeDotationRepository.save(demande);
    }
}
