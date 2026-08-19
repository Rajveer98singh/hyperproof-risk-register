package com.hyperproof.riskregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyperproof.riskregister.domain.Category;
import com.hyperproof.riskregister.domain.Risk;
import com.hyperproof.riskregister.domain.Status;
import com.hyperproof.riskregister.web.dto.RiskRequest;
import com.hyperproof.riskregister.web.dto.RiskResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the {@code @Retryable} optimistic-lock wiring: the update retries up to 3 times on a
 * version conflict and gives up (propagating) once attempts are exhausted.
 */
@SpringBootTest
@ActiveProfiles("test")
class RiskServiceRetryTest {

    @Autowired
    private RiskService riskService;

    @MockBean
    private com.hyperproof.riskregister.repository.RiskRepository riskRepository;

    private RiskRequest req() {
        return new RiskRequest("T", "d", Category.SECURITY, "o", 3, 3, Status.OPEN, null, null);
    }

    @Test
    void retriesThenSucceedsOnTransientVersionConflict() {
        Risk risk = new Risk("T", "d", Category.SECURITY, "o", 3, 3, Status.OPEN, null);
        when(riskRepository.findById(1L)).thenReturn(Optional.of(risk));
        when(riskRepository.save(any(Risk.class)))
                .thenThrow(new OptimisticLockingFailureException("conflict"))  // attempt 1
                .thenThrow(new OptimisticLockingFailureException("conflict"))  // attempt 2
                .thenReturn(risk);                                             // attempt 3 succeeds

        RiskResponse result = riskService.update(1L, req());

        assertThat(result.title()).isEqualTo("T");
        verify(riskRepository, times(3)).save(any(Risk.class));
    }

    @Test
    void propagatesAfterExhaustingRetries() {
        Risk risk = new Risk("T", "d", Category.SECURITY, "o", 3, 3, Status.OPEN, null);
        when(riskRepository.findById(1L)).thenReturn(Optional.of(risk));
        when(riskRepository.save(any(Risk.class)))
                .thenThrow(new OptimisticLockingFailureException("conflict"));

        assertThatThrownBy(() -> riskService.update(1L, req()))
                .isInstanceOf(OptimisticLockingFailureException.class);
        verify(riskRepository, times(3)).save(any(Risk.class));
    }
}
