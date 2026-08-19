package com.hyperproof.riskregister.domain;

/** Risk category. Stored as VARCHAR (via {@code @Enumerated(STRING)}). */
public enum Category {
    OPERATIONAL,
    FINANCIAL,
    COMPLIANCE,
    SECURITY,
    STRATEGIC
}
