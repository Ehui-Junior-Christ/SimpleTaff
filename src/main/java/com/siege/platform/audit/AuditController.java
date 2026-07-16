package com.siege.platform.audit;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'SUPER_ADMIN')")
public class AuditController {
    private final AuditLogRepository repository;

    public AuditController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AuditLog> list(@RequestParam(required = false) LocalDate dateDebut,
                               @RequestParam(required = false) LocalDate dateFin) {
        if (dateDebut == null || dateFin == null) {
            return repository.findAll();
        }
        return repository.findByCreeLeBetweenOrderByCreeLeDesc(dateDebut.atStartOfDay(), dateFin.plusDays(1).atStartOfDay());
    }
}
