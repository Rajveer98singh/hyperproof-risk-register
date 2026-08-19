package com.hyperproof.riskregister.service.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperproof.riskregister.domain.Severity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ScoringServiceTest {

    private final ScoringService scoring = new ScoringService(new MaxEffectivenessStrategy());

    @Nested
    @DisplayName("inherent score")
    class Inherent {
        @Test
        void isLikelihoodTimesImpact() {
            assertThat(scoring.inherent(1, 1)).isEqualTo(1);
            assertThat(scoring.inherent(5, 5)).isEqualTo(25);
            assertThat(scoring.inherent(3, 4)).isEqualTo(12);
        }
    }

    @Nested
    @DisplayName("severity bands (Low 1-5, Medium 6-12, High 13-19, Critical 20-25)")
    class SeverityBands {
        @ParameterizedTest(name = "score {0} -> {1}")
        @CsvSource({
                "1, LOW", "5, LOW",
                "6, MEDIUM", "12, MEDIUM",
                "13, HIGH", "19, HIGH",
                "20, CRITICAL", "25, CRITICAL"
        })
        void boundaries(int score, Severity expected) {
            assertThat(scoring.severity(score)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("residual sanity checks required by the brief")
    class ResidualSanity {
        @Test
        @DisplayName("a risk with zero mitigations has residual == inherent")
        void zeroMitigations() {
            assertThat(scoring.residual(5, 5, List.of())).isEqualTo(25);
        }

        @Test
        @DisplayName("one highly effective mitigation ends up meaningfully lower than inherent")
        void oneStrongMitigation() {
            int inherent = scoring.inherent(5, 5);          // 25
            int residual = scoring.residual(5, 5, List.of(5)); // 5
            assertThat(residual).isLessThan(inherent);
            assertThat(inherent - residual).isGreaterThanOrEqualTo(10); // "meaningful"
        }

        @Test
        @DisplayName("residual never falls below 1")
        void neverBelowOne() {
            assertThat(scoring.residual(1, 1, List.of(5))).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("combined score bundle")
    class Bundle {
        @Test
        void computesAllFourValues() {
            Scores s = scoring.score(5, 5, List.of(5));
            assertThat(s.inherentScore()).isEqualTo(25);
            assertThat(s.inherentSeverity()).isEqualTo(Severity.CRITICAL);
            assertThat(s.residualScore()).isEqualTo(5);
            assertThat(s.residualSeverity()).isEqualTo(Severity.LOW);
        }
    }
}
