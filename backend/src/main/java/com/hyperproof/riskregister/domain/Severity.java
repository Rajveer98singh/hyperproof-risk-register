package com.hyperproof.riskregister.domain;

/**
 * Severity band for a score, used for display on both inherent and residual scores.
 *
 * <p>Bands (inclusive): Low 1–5, Medium 6–12, High 13–19, Critical 20–25.
 */
public enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /**
     * Maps a 1–25 score to its severity band. Values outside 1–25 are clamped to the nearest
     * band (defensive; callers should already pass valid scores).
     */
    public static Severity fromScore(int score) {
        if (score >= 20) return CRITICAL;
        if (score >= 13) return HIGH;
        if (score >= 6)  return MEDIUM;
        return LOW;
    }
}
