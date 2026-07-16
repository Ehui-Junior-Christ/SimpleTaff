package com.siege.platform.common;

import com.siege.platform.entreprise.Entreprise;
import com.siege.platform.utilisateur.Utilisateur;
import com.siege.platform.utilisateur.UtilisateurRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentTenantService {

    private final UtilisateurRepository utilisateurRepository;

    public CurrentTenantService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public Entreprise entreprise() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non connecte."));
        if (user.getEntreprise() == null) {
            throw new IllegalArgumentException("Aucune entreprise associee.");
        }
        return user.getEntreprise();
    }
}
