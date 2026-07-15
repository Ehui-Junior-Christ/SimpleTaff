package com.siege.platform.facturation;

import com.siege.platform.paie.BulletinDePaieRepository;
import com.siege.platform.poste.Affectation;
import com.siege.platform.structuredemandeuse.StructureDemandeuse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class FactureService {

    private final FactureRepository factureRepository;
    private final BulletinDePaieRepository bulletinDePaieRepository;

    public FactureService(FactureRepository factureRepository, BulletinDePaieRepository bulletinDePaieRepository) {
        this.factureRepository = factureRepository;
        this.bulletinDePaieRepository = bulletinDePaieRepository;
    }

    @Transactional
    public Facture genererFacture(StructureDemandeuse client, String periode, BigDecimal montant, 
                                  String rapportUrl, Set<Affectation> affectationsConcernees) {
        
        // Validation métier critique (Section 6.4) : Ordre strict impératif
        if (rapportUrl == null || rapportUrl.isBlank()) {
            throw new IllegalArgumentException("Le rapport de pointage PDF est obligatoire pour émettre la facture.");
        }

        for (Affectation aff : affectationsConcernees) {
            boolean bulletinExiste = bulletinDePaieRepository.existsByAffectationIdAndPeriode(aff.getId(), periode);
            if (!bulletinExiste) {
                throw new IllegalStateException("Impossible d'émettre la facture : Le bulletin de paie pour l'affectation " 
                        + aff.getId() + " n'a pas été clôturé pour la période " + periode);
            }
        }

        Facture facture = new Facture();
        facture.setEntreprise(client.getEntreprise());
        facture.setStructureDemandeuse(client);
        facture.setPeriode(periode);
        facture.setMontantFacture(montant);
        facture.setRapportPointageUrl(rapportUrl);
        facture.setStatutPaiement("EN_ATTENTE");
        facture.setAffectations(affectationsConcernees);

        return factureRepository.save(facture);
    }
}
