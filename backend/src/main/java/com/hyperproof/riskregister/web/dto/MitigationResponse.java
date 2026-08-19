package com.hyperproof.riskregister.web.dto;

import java.time.Instant;

public record MitigationResponse(
        Long id,
        String description,
        int effectiveness,
        Instant createdAt) {
}
