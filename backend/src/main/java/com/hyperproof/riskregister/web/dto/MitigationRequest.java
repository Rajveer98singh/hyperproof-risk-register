package com.hyperproof.riskregister.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MitigationRequest(
        @NotBlank String description,
        @NotNull @Min(1) @Max(5) Integer effectiveness) {
}
