package com.hyperproof.riskregister.service.scoring;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Default residual-scoring strategy.
 *
 * <p>Combines mitigations by their <b>maximum</b> effectiveness — a risk's real protection is its
 * single strongest applicable control. This is deliberately chosen over:
 * <ul>
 *   <li>a multiplicative decay {@code ∏(1 - effᵢ/5)}, which lets many weak controls drive residual
 *       toward zero and <i>overstates</i> protection (an auditor-hostile artifact); and</li>
 *   <li>an average, which <i>dilutes</i> a strong control diluted by weaker ones.</li>
 * </ul>
 *
 * <p>Formula:
 * <pre>
 *   reductionFactor = (maxEffectiveness / 5) * 0.8    // eff 5 → 0.80, eff 1 → 0.16, none → 0
 *   residual        = clamp( round( inherent * (1 - reductionFactor) ), 1, inherent )
 * </pre>
 *
 * The 0.8 cap encodes that no control is perfect: residual risk always remains &gt; 0. The clamp
 * guarantees residual never drops below 1 and never exceeds inherent.
 */
@Component
public class MaxEffectivenessStrategy implements ResidualScoringStrategy {

    /** Maximum fraction of inherent risk a single mitigation can remove. */
    private static final double MAX_REDUCTION = 0.8;
    private static final int MAX_EFFECTIVENESS = 5;

    @Override
    public int residual(int inherentScore, List<Integer> effectivenesses) {
        if (effectivenesses == null || effectivenesses.isEmpty()) {
            return inherentScore; // no mitigations -> residual equals inherent
        }

        int maxEffectiveness = effectivenesses.stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        double reductionFactor = ((double) maxEffectiveness / MAX_EFFECTIVENESS) * MAX_REDUCTION;
        long residual = Math.round(inherentScore * (1 - reductionFactor));

        // Never below 1, never above inherent.
        return (int) Math.max(1, Math.min(inherentScore, residual));
    }
}
