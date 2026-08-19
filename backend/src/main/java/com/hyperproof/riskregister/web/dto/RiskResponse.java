package com.hyperproof.riskregister.web.dto;

import com.hyperproof.riskregister.domain.Category;
import com.hyperproof.riskregister.domain.Severity;
import com.hyperproof.riskregister.domain.Status;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Full risk representation returned by the API. Score fields are computed on every read and never
 * persisted, so they cannot drift from the raw inputs.
 */
public record RiskResponse(
        Long id,
        String title,
        String description,
        Category category,
        String owner,
        int likelihood,
        int impact,
        Status status,
        int inherentScore,
        Severity inherentSeverity,
        int residualScore,
        Severity residualSeverity,
        int mitigationCount,
        LocalDate nextReviewDate,
        boolean overdue,
        List<MitigationResponse> mitigations,
        List<FrameworkResponse> frameworks,
        Instant createdAt,
        Instant updatedAt) {
}
