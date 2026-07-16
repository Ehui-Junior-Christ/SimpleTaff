package com.siege.platform.pointage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarteAgentRepository extends JpaRepository<CarteAgent, UUID> {
    Optional<CarteAgent> findByCodeQrAndStatut(String codeQr, String statut);

    Optional<CarteAgent> findByIdentifiantNfcAndStatut(String identifiantNfc, String statut);

    Optional<CarteAgent> findBySourceBiometrieAndStatut(String sourceBiometrie, String statut);

}
