package com.hyperproof.riskregister.service;

import com.hyperproof.riskregister.domain.Category;
import com.hyperproof.riskregister.domain.FrameworkControl;
import com.hyperproof.riskregister.domain.Risk;
import com.hyperproof.riskregister.domain.Status;
import com.hyperproof.riskregister.error.BusinessRuleException;
import com.hyperproof.riskregister.error.NotFoundException;
import com.hyperproof.riskregister.repository.FrameworkControlRepository;
import com.hyperproof.riskregister.repository.RiskRepository;
import com.hyperproof.riskregister.repository.spec.RiskSpecifications;
import com.hyperproof.riskregister.service.scoring.ScoringService;
import com.hyperproof.riskregister.web.dto.RiskRequest;
import com.hyperproof.riskregister.web.dto.RiskResponse;
import com.hyperproof.riskregister.web.mapper.RiskMapper;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for risks. Returns fully-mapped {@link RiskResponse} DTOs (mapping happens
 * inside the transaction) so that lazy associations load safely with open-in-view disabled, and
 * controllers stay thin.
 */
@Service
public class RiskService {

    private final RiskRepository riskRepository;
    private final FrameworkControlRepository frameworkRepository;
    private final ScoringService scoringService;
    private final RiskMapper mapper;

    public RiskService(RiskRepository riskRepository,
                       FrameworkControlRepository frameworkRepository,
                       ScoringService scoringService,
                       RiskMapper mapper) {
        this.riskRepository = riskRepository;
        this.frameworkRepository = frameworkRepository;
        this.scoringService = scoringService;
        this.mapper = mapper;
    }

    @Transactional
    public RiskResponse create(RiskRequest req) {
        Risk risk = new Risk(req.title(), req.description(), req.category(), req.owner(),
                req.likelihood(), req.impact(), req.status(), req.nextReviewDate());
        risk.setFrameworks(resolveFrameworks(req.frameworkControlIds()));
        validateClosable(risk);
        return mapper.toResponse(riskRepository.save(risk));
    }

    @Transactional(readOnly = true)
    public List<RiskResponse> list(Category category, Status status, String sort) {
        Specification<Risk> spec = RiskSpecifications.hasCategory(category)
                .and(RiskSpecifications.hasStatus(status));
        List<Risk> risks = riskRepository.findAll(spec);

        if ("residualDesc".equalsIgnoreCase(sort)) {
            // Residual is a derived field (not a column), so we sort in the service. Fine at
            // register scale; a materialized column would be the move for very large registers.
            risks = risks.stream()
                    .sorted(Comparator.comparingInt(this::residualOf).reversed())
                    .toList();
        }
        return risks.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RiskResponse get(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    /**
     * Updates a risk. Retries transparently on a concurrent version conflict: each attempt runs in a
     * fresh transaction that re-reads the latest row, re-applies the change, and commits. If all
     * attempts lose the race, the exception propagates and is mapped to HTTP 409.
     */
    @Retryable(retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public RiskResponse update(Long id, RiskRequest req) {
        Risk risk = getEntity(id);
        risk.setTitle(req.title());
        risk.setDescription(req.description());
        risk.setCategory(req.category());
        risk.setOwner(req.owner());
        risk.setLikelihood(req.likelihood());
        risk.setImpact(req.impact());
        risk.setStatus(req.status());
        risk.setNextReviewDate(req.nextReviewDate());
        risk.setFrameworks(resolveFrameworks(req.frameworkControlIds()));
        validateClosable(risk);
        return mapper.toResponse(riskRepository.save(risk));
    }

    @Transactional
    public void delete(Long id) {
        if (!riskRepository.existsById(id)) {
            throw new NotFoundException("Risk", id);
        }
        riskRepository.deleteById(id);
    }

    private Risk getEntity(Long id) {
        return riskRepository.findById(id).orElseThrow(() -> new NotFoundException("Risk", id));
    }

    /**
     * Business rule: a risk may not be Closed with zero mitigations. A compliance product must not
     * record a risk as resolved with no evidence any control was ever applied — that is exactly the
     * audit gap reviewers look for. Once at least one mitigation exists, closing is allowed
     * (accepting the residual risk is a valid, logged decision).
     */
    private void validateClosable(Risk risk) {
        if (risk.getStatus() == Status.CLOSED && risk.getMitigations().isEmpty()) {
            throw new BusinessRuleException(
                    "Cannot close a risk with no mitigations. Add at least one mitigation before closing.");
        }
    }

    private Set<FrameworkControl> resolveFrameworks(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<FrameworkControl> found = frameworkRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new NotFoundException("FrameworkControl", ids);
        }
        return new LinkedHashSet<>(found);
    }

    private int residualOf(Risk risk) {
        return scoringService.residual(risk.getLikelihood(), risk.getImpact(),
                risk.getMitigations().stream().map(m -> m.getEffectiveness()).toList());
    }
}
