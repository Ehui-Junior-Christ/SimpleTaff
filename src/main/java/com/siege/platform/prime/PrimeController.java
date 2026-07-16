package com.siege.platform.prime;

import com.siege.platform.common.CurrentTenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/primes")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'SUPER_ADMIN')")
public class PrimeController {
    private final ReglePrimeRendementRepository repository;
    private final CurrentTenantService tenantService;

    public PrimeController(ReglePrimeRendementRepository repository, CurrentTenantService tenantService) {
        this.repository = repository;
        this.tenantService = tenantService;
    }

    @GetMapping("/rendement/regles")
    public List<ReglePrimeRendement> regles() {
        return repository.findAll();
    }

    @PostMapping("/rendement/regles")
    public ResponseEntity<?> save(@RequestBody ReglePrimeRendement regle) {
        regle.setEntreprise(tenantService.entreprise());
        return ResponseEntity.ok(repository.save(regle));
    }

    @PostMapping("/rendement/simuler")
    public Map<String, Object> simuler(@RequestBody Map<String, Object> payload) {
        BigDecimal montantParPoint = new BigDecimal(payload.getOrDefault("montantParPoint", "0").toString());
        int score = Integer.parseInt(payload.getOrDefault("score", "0").toString());
        return Map.of("score", score, "montantCalcule", montantParPoint.multiply(BigDecimal.valueOf(score)));
    }
}
