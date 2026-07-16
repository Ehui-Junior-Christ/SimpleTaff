package com.siege.platform.evaluation;

import com.siege.platform.agent.AgentTerrainRepository;
import com.siege.platform.common.CurrentTenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/evaluations")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'COORDONNATEUR', 'SUPER_ADMIN')")
public class EvaluationController {
    private final EvaluationAgentRepository evaluationRepository;
    private final AgentTerrainRepository agentRepository;
    private final CurrentTenantService tenantService;

    public EvaluationController(EvaluationAgentRepository evaluationRepository,
                                AgentTerrainRepository agentRepository,
                                CurrentTenantService tenantService) {
        this.evaluationRepository = evaluationRepository;
        this.agentRepository = agentRepository;
        this.tenantService = tenantService;
    }

    @GetMapping
    public List<EvaluationAgent> list(@RequestParam(required = false) UUID agentId) {
        return agentId == null ? evaluationRepository.findAll() : evaluationRepository.findByAgentIdOrderByAnneeDesc(agentId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody EvaluationAgent evaluation) {
        evaluation.setEntreprise(tenantService.entreprise());
        evaluation.setAgent(agentRepository.findById(evaluation.getAgent().getId()).orElseThrow());
        evaluation.setDateEvaluation(LocalDate.now());
        evaluation.setScoreTotal(
                evaluation.getPonctualite()
                        + evaluation.getDiscipline()
                        + evaluation.getQualite()
                        + evaluation.getProductivite()
                        + evaluation.getEspritEquipe()
                        + evaluation.getRespectProcedures()
                        + evaluation.getSatisfactionClient()
                        + evaluation.getCommunication()
        );
        return ResponseEntity.ok(evaluationRepository.save(evaluation));
    }
}
