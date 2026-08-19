package com.hyperproof.riskregister.service.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Focused tests for the default residual formula in isolation. */
class MaxEffectivenessStrategyTest {

    private final MaxEffectivenessStrategy strategy = new MaxEffectivenessStrategy();

    @Test
    @DisplayName("no mitigations -> residual equals inherent")
    void noMitigations() {
        assertThat(strategy.residual(25, List.of())).isEqualTo(25);
        assertThat(strategy.residual(25, null)).isEqualTo(25);
        assertThat(strategy.residual(7, List.of())).isEqualTo(7);
    }

    @Test
    @DisplayName("uses the maximum effectiveness, not the sum or average")
    void usesMaxNotSum() {
        // Three weak-to-strong controls: only the strongest (5) should count.
        assertThat(strategy.residual(16, List.of(2, 5, 3)))
                .isEqualTo(strategy.residual(16, List.of(5)));
    }

    @Test
    @DisplayName("residual is clamped to a floor of 1 and never exceeds inherent")
    void clampBounds() {
        assertThat(strategy.residual(1, List.of(5))).isEqualTo(1); // would round to 0
        assertThat(strategy.residual(3, List.of(5))).isGreaterThanOrEqualTo(1);
        // A mitigation can never make residual larger than inherent.
        for (int inherent = 1; inherent <= 25; inherent++) {
            for (int eff = 1; eff <= 5; eff++) {
                int r = strategy.residual(inherent, List.of(eff));
                assertThat(r).isBetween(1, inherent);
            }
        }
    }

    @ParameterizedTest(name = "inherent={0}, maxEff={1} -> residual={2}")
    @CsvSource({
            "25, 5, 5",   // strongest control cuts a Critical risk to Low
            "25, 1, 21",  // weakest control barely moves it
            "16, 5, 3",
            "6,  3, 3",
            "12, 4, 4",   // (4/5*0.8)=0.64 -> 12*0.36=4.32 -> 4
            "20, 2, 14"   // (2/5*0.8)=0.32 -> 20*0.68=13.6 -> 14 (round half up)
    })
    void workedExamples(int inherent, int maxEff, int expected) {
        assertThat(strategy.residual(inherent, List.of(maxEff))).isEqualTo(expected);
    }
}
