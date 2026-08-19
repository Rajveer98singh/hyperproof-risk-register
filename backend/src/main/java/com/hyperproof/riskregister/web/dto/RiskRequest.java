package com.hyperproof.riskregister.web.dto;

import com.hyperproof.riskregister.domain.Category;
import com.hyperproof.riskregister.domain.Status;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;

/**
 * Create/update payload for a risk. Wrapper types (Integer) are used for likelihood/impact so a
 * missing value fails {@code @NotNull} validation instead of silently defaulting to 0.
 */
public record RiskRequest(
        @NotBlank String title,
        String description,
        @NotNull Category category,
        String owner,
        @NotNull @Min(1) @Max(5) Integer likelihood,
        @NotNull @Min(1) @Max(5) Integer impact,
        @NotNull Status status,
        LocalDate nextReviewDate,
        Set<Long> frameworkControlIds) {
}
