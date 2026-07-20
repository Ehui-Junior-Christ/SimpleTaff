package com.siege.platform.evaluation;
 
import com.siege.platform.agent.AgentTerrainRepository;
import com.siege.platform.common.CurrentTenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
 
import java.time.LocalDate;
import java.util.*;
 
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/evaluations")
@PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'COORDONNATEUR', 'SUPER_ADMIN', 'EMPLOYEUR')")
@Transactional
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
        UUID entrepriseId = tenantService.entreprise().getId();
        return agentId == null
                ? evaluationRepository.findByEntrepriseIdWithAgent(entrepriseId)
                : evaluationRepository.findByAgentIdOrderByAnneeDesc(agentId);
    }
 
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEUR', 'SUPER_ADMIN')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> payload) {
        EvaluationAgent evaluation = new EvaluationAgent();
        evaluation.setEntreprise(tenantService.entreprise());
        
        UUID agentId;
        if (payload.get("agentId") != null) {
            agentId = UUID.fromString(payload.get("agentId").toString());
        } else if (payload.get("agent") != null && ((Map<?,?>)payload.get("agent")).get("id") != null) {
            agentId = UUID.fromString(((Map<?,?>)payload.get("agent")).get("id").toString());
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "agentId est requis"));
        }
        
        evaluation.setAgent(agentRepository.findById(agentId).orElseThrow());
        evaluation.setAnnee(Integer.parseInt(payload.getOrDefault("annee", Calendar.getInstance().get(Calendar.YEAR)).toString()));
        evaluation.setDateEvaluation(LocalDate.now());
        
        evaluation.setPonctualite(Integer.parseInt(payload.getOrDefault("ponctualite", 0).toString()));
        evaluation.setDiscipline(Integer.parseInt(payload.getOrDefault("discipline", 0).toString()));
        evaluation.setQualite(Integer.parseInt(payload.getOrDefault("qualite", 0).toString()));
        evaluation.setProductivite(Integer.parseInt(payload.getOrDefault("productivite", 0).toString()));
        evaluation.setEspritEquipe(Integer.parseInt(payload.getOrDefault("espritEquipe", 0).toString()));
        evaluation.setRespectProcedures(Integer.parseInt(payload.getOrDefault("respectProcedures", 0).toString()));
        evaluation.setSatisfactionClient(Integer.parseInt(payload.getOrDefault("satisfactionClient", 0).toString()));
        evaluation.setCommunication(Integer.parseInt(payload.getOrDefault("communication", 0).toString()));
        
        evaluation.setCommentaire((String) payload.get("commentaire"));
        
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
