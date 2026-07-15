package com.siege.platform.paie;

import com.siege.platform.entreprise.Entreprise;
import com.siege.platform.poste.Affectation;
import com.siege.platform.poste.Poste;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class PaieCalculService {

    private final BulletinDePaieRepository bulletinDePaieRepository;

    public PaieCalculService(BulletinDePaieRepository bulletinDePaieRepository) {
        this.bulletinDePaieRepository = bulletinDePaieRepository;
    }

    @Transactional
    public BulletinDePaie genererBulletin(Affectation affectation, String periode, 
                                          int joursPrevus, int joursValides, 
                                          int joursAbsJustifieeCourte, int joursAbsJustifieeLongue, 
                                          int joursAbsNonJustifiee, int joursCongePaye) {
        
        Poste poste = affectation.getPoste();
        Entreprise entreprise = affectation.getEntreprise();

        BigDecimal salaireBrutNegocie = poste.getSalaireBrutNegocie();
        BigDecimal retenueForfaitaire = poste.getMontantRetenueForfaitaire();
        
        // 1. Déduction pour absences NON JUSTIFIÉES (déduction complète)
        BigDecimal deductionNonJustifiee = retenueForfaitaire.multiply(BigDecimal.valueOf(joursAbsNonJustifiee));
        
        // 2. Déduction pour absences JUSTIFIÉES LONGUES (au-delà du seuil)
        BigDecimal deductionJustifieeLongue = BigDecimal.ZERO;
        if (joursAbsJustifieeLongue >= entreprise.getSeuilAbsenceLongueJours()) {
            // Taux de retenue réduite (ex: 25%) -> on déduit 25% de la retenue forfaitaire par jour
            BigDecimal tauxReduction = entreprise.getTauxRetenueReduite().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal retenueReduiteParJour = retenueForfaitaire.multiply(tauxReduction);
            
            int joursAuDelaDuSeuil = joursAbsJustifieeLongue - entreprise.getSeuilAbsenceLongueJours();
            // NB: La consigne dit "au-delà du seuil". Si on compte le seuil inclus ou exclus, on va appliquer pour les jours excédentaires.
            // On supposera ici que la retenue s'applique à tous les jours d'absence justifiée longue (ou juste ceux au delà). 
            // "au-delà du seuil × (retenue forfaitaire × tauxRetenueReduite)" -> on multiplie par le nombre total de jours longue durée
            deductionJustifieeLongue = retenueReduiteParJour.multiply(BigDecimal.valueOf(joursAbsJustifieeLongue));
        }

        // 3. Salaire Brut Effectif
        BigDecimal salaireBrutEffectif = salaireBrutNegocie
                .subtract(deductionNonJustifiee)
                .subtract(deductionJustifieeLongue);
        
        // Plancher à 0
        if (salaireBrutEffectif.compareTo(BigDecimal.ZERO) < 0) {
            salaireBrutEffectif = BigDecimal.ZERO;
        }

        // 4. Salaire Net Calculé (Taux forfaitaire de cotisation)
        BigDecimal tauxCotisation = entreprise.getTauxCotisation().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal montantCotisation = salaireBrutEffectif.multiply(tauxCotisation);
        BigDecimal salaireNetCalcule = salaireBrutEffectif.subtract(montantCotisation).setScale(2, RoundingMode.HALF_UP);

        BulletinDePaie bulletin = new BulletinDePaie();
        bulletin.setEntreprise(entreprise);
        bulletin.setAgent(affectation.getAgent());
        bulletin.setAffectation(affectation);
        bulletin.setPeriode(periode);
        bulletin.setJoursPrevus(joursPrevus);
        bulletin.setJoursValides(joursValides);
        bulletin.setJoursAbsenceJustifieeCourte(joursAbsJustifieeCourte);
        bulletin.setJoursAbsenceJustifieeLongue(joursAbsJustifieeLongue);
        bulletin.setJoursAbsenceNonJustifiee(joursAbsNonJustifiee);
        bulletin.setJoursCongePaye(joursCongePaye);
        bulletin.setSalaireBrutEffectif(salaireBrutEffectif.setScale(2, RoundingMode.HALF_UP));
        bulletin.setSalaireNetCalcule(salaireNetCalcule);
        bulletin.setDateCloture(LocalDateTime.now());
        bulletin.setStatutPaiement("EN_ATTENTE");

        return bulletinDePaieRepository.save(bulletin);
    }
}
