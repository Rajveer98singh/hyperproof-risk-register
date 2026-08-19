package com.hyperproof.riskregister.service.scoring;

import com.hyperproof.riskregister.domain.Severity;

/** Immutable bundle of the four computed scoring values for a risk. */
public record Scores(
        int inherentScore,
        Severity inherentSeverity,
        int residualScore,
        Severity residualSeverity) {
}
