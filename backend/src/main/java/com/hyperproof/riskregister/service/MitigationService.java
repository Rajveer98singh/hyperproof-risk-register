package com.hyperproof.riskregister.service;

import com.hyperproof.riskregister.domain.Mitigation;
import com.hyperproof.riskregister.domain.Risk;
import com.hyperproof.riskregister.error.NotFoundException;
import com.hyperproof.riskregister.repository.MitigationRepository;
import com.hyperproof.riskregister.repository.RiskRepository;
import com.hyperproof.riskregister.web.dto.RiskResponse;
import com.hyperproof.riskregister.web.mapper.RiskMapper;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mitigation operations. Each returns the parent {@link Risk} so the client receives the updated
 * residual score in a single round-trip.
 */
@Service
public class MitigationService {

    private final RiskRepository riskRepository;
    private final MitigationRepository mitigationRepository;
    private final RiskMapper mapper;

    public MitigationService(RiskRepository riskRepository, MitigationRepository mitigationRepository,
                             RiskMapper mapper) {
        this.riskRepository = riskRepository;
        this.mitigationRepository = mitigationRepository;
        this.mapper = mapper;
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public RiskResponse addMitigation(Long riskId, String description, int effectiveness) {
        Risk risk = riskRepository.findById(riskId)
                .orElseThrow(() -> new NotFoundException("Risk", riskId));
        risk.addMitigation(new Mitigation(description, effectiveness));
        return mapper.toResponse(riskRepository.save(risk));
    }

    @Transactional
    public RiskResponse updateMitigation(Long mitigationId, String description, int effectiveness) {
        Mitigation mitigation = mitigationRepository.findById(mitigationId)
                .orElseThrow(() -> new NotFoundException("Mitigation", mitigationId));
        mitigation.setDescription(description);
        mitigation.setEffectiveness(effectiveness);
        mitigationRepository.save(mitigation);
        return mapper.toResponse(mitigation.getRisk());
    }

    @Transactional
    public void deleteMitigation(Long mitigationId) {
        Mitigation mitigation = mitigationRepository.findById(mitigationId)
                .orElseThrow(() -> new NotFoundException("Mitigation", mitigationId));
        // Remove from the parent's collection so orphanRemoval deletes the row.
        Risk risk = mitigation.getRisk();
        risk.removeMitigation(mitigation);
        riskRepository.save(risk);
    }
}
