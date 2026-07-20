package com.siege.platform.notification;
 
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
import java.util.Map;
 
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'COORDONNATEUR', 'SUPER_ADMIN')")
@Transactional
public class NotificationController {
    private final NotificationEvenementRepository repository;
 
    public NotificationController(NotificationEvenementRepository repository) {
        this.repository = repository;
    }
 
    @GetMapping
    public List<NotificationEvenement> getNotifications() {
        return repository.findAllByOrderByCreeLeDesc();
    }
 
    @PostMapping("/{id}/lu")
    public ResponseEntity<?> marquerLu(@PathVariable UUID id) {
        NotificationEvenement event = repository.findById(id).orElseThrow();
        event.setStatut("LU");
        repository.save(event);
        return ResponseEntity.ok(Map.of("message", "Notification marquée comme lue."));
    }
 
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Notification supprimée."));
    }
}
