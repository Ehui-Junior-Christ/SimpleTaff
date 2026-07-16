package com.siege.platform.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationEvenementRepository extends JpaRepository<NotificationEvenement, UUID> {
}
