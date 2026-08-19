package com.hyperproof.riskregister.web.mapper;

import com.hyperproof.riskregister.domain.FrameworkControl;
import com.hyperproof.riskregister.domain.Mitigation;
import com.hyperproof.riskregister.domain.Risk;
import com.hyperproof.riskregister.service.scoring.Scores;
import com.hyperproof.riskregister.service.scoring.ScoringService;
import com.hyperproof.riskregister.web.dto.FrameworkResponse;
import com.hyperproof.riskregister.web.dto.MitigationResponse;
import com.hyperproof.riskregister.web.dto.RiskResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Maps {@link Risk} entities to {@link RiskResponse} DTOs, injecting freshly computed scores.
 * Keeping this the single conversion point guarantees the API and UI never see a stale score.
 */
@Component
public class RiskMapper {

    private final ScoringService scoringService;
    private final Clock clock;

    public RiskMapper(ScoringService scoringService, Clock clock) {
        this.scoringService = scoringService;
        this.clock = clock;
    }

    public RiskResponse toResponse(Risk risk) {
        List<Integer> effectivenesses = risk.getMitigations().stream()
                .map(Mitigation::getEffectiveness)
                .toList();

        Scores scores = scoringService.score(risk.getLikelihood(), risk.getImpact(), effectivenesses);

        List<MitigationResponse> mitigations = risk.getMitigations().stream()
                .map(m -> new MitigationResponse(
                        m.getId(), m.getDescription(), m.getEffectiveness(), m.getCreatedAt()))
                .toList();

        List<FrameworkResponse> frameworks = risk.getFrameworks().stream()
                .map(this::toFrameworkResponse)
                .toList();

        boolean overdue = risk.getNextReviewDate() != null
                && risk.getNextReviewDate().isBefore(LocalDate.now(clock));

        return new RiskResponse(
                risk.getId(),
                risk.getTitle(),
                risk.getDescription(),
                risk.getCategory(),
                risk.getOwner(),
                risk.getLikelihood(),
                risk.getImpact(),
                risk.getStatus(),
                scores.inherentScore(),
                scores.inherentSeverity(),
                scores.residualScore(),
                scores.residualSeverity(),
                mitigations.size(),
                risk.getNextReviewDate(),
                overdue,
                mitigations,
                frameworks,
                risk.getCreatedAt(),
                risk.getUpdatedAt());
    }

    public FrameworkResponse toFrameworkResponse(FrameworkControl fc) {
        return new FrameworkResponse(fc.getId(), fc.getFramework(), fc.getCode(), fc.getName());
    }
}
