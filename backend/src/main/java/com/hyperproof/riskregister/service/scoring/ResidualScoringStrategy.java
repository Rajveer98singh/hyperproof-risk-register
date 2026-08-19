package com.hyperproof.riskregister.service.scoring;

import java.util.List;

/**
 * Strategy for reducing an inherent risk score to a residual score given the effectiveness of the
 * mitigations applied to the risk.
 *
 * <p>Extracted behind an interface so the residual formula is a single, swappable, independently
 * testable seam. See {@link MaxEffectivenessStrategy} for the default; the README documents
 * alternative formulas (e.g. a diminishing-returns blend) that could plug in here.
 */
public interface ResidualScoringStrategy {

    /**
     * @param inherentScore   inherent score, 1–25
     * @param effectivenesses effectiveness (1–5) of each mitigation on the risk; empty if none
     * @return residual score, always within {@code [1, inherentScore]}
     */
    int residual(int inherentScore, List<Integer> effectivenesses);
}
