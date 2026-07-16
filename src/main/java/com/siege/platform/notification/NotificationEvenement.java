package com.siege.platform.notification;

import com.siege.platform.entreprise.Entreprise;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_evenement")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "entreprise_id = :entrepriseId")
public class NotificationEvenement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String canal = "WEBHOOK";

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String statut = "A_ENVOYER";

    @Column(nullable = false)
    private LocalDateTime creeLe = LocalDateTime.now();
}
