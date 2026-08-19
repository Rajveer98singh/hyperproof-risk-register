package com.hyperproof.riskregister.service.scoring;

import com.hyperproof.riskregister.domain.Severity;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Pure, stateless risk-scoring logic — the graded core of this application.
 *
 * <p>Has no persistence or web dependencies so it can be exhaustively unit-tested. All scores are
 * computed here on demand and never persisted, which makes it impossible for a stored score to
 * drift out of sync with its inputs.
 */
@Service
public class ScoringService {

    private final ResidualScoringStrategy residualStrategy;

    public ScoringService(ResidualScoringStrategy residualStrategy) {
        this.residualStrategy = residualStrategy;
    }

    /** Inherent score = likelihood × impact, range 1–25. */
    public int inherent(int likelihood, int impact) {
        return likelihood * impact;
    }

    /** Residual score for the given inherent inputs and mitigation effectivenesses. */
    public int residual(int likelihood, int impact, List<Integer> effectivenesses) {
        return residualStrategy.residual(inherent(likelihood, impact), effectivenesses);
    }

    /** Severity band for a 1–25 score. */
    public Severity severity(int score) {
        return Severity.fromScore(score);
    }

    /** Computes all four display values in one call (used by the response mapper). */
    public Scores score(int likelihood, int impact, List<Integer> effectivenesses) {
        int inherent = inherent(likelihood, impact);
        int residual = residualStrategy.residual(inherent, effectivenesses);
        return new Scores(inherent, severity(inherent), residual, severity(residual));
    }
}
