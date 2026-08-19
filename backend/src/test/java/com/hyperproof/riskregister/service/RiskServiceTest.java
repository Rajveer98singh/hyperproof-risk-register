package com.hyperproof.riskregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyperproof.riskregister.domain.Category;
import com.hyperproof.riskregister.domain.Mitigation;
import com.hyperproof.riskregister.domain.Risk;
import com.hyperproof.riskregister.domain.Status;
import com.hyperproof.riskregister.error.BusinessRuleException;
import com.hyperproof.riskregister.error.NotFoundException;
import com.hyperproof.riskregister.repository.FrameworkControlRepository;
import com.hyperproof.riskregister.repository.RiskRepository;
import com.hyperproof.riskregister.service.scoring.MaxEffectivenessStrategy;
import com.hyperproof.riskregister.service.scoring.ScoringService;
import com.hyperproof.riskregister.web.dto.RiskRequest;
import com.hyperproof.riskregister.web.dto.RiskResponse;
import com.hyperproof.riskregister.web.mapper.RiskMapper;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RiskServiceTest {

    private RiskRepository riskRepository;
    private FrameworkControlRepository frameworkRepository;
    private RiskService service;

    @BeforeEach
    void setUp() {
        riskRepository = Mockito.mock(RiskRepository.class);
        frameworkRepository = Mockito.mock(FrameworkControlRepository.class);
        ScoringService scoring = new ScoringService(new MaxEffectivenessStrategy());
        RiskMapper mapper = new RiskMapper(scoring, Clock.systemUTC());
        service = new RiskService(riskRepository, frameworkRepository, scoring, mapper);
        when(riskRepository.save(any(Risk.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private RiskRequest req(Status status) {
        return new RiskRequest("Title", "desc", Category.SECURITY, "owner", 3, 3, status, null, null);
    }

    @Test
    void rejectsClosingRiskWithNoMitigations() {
        assertThatThrownBy(() -> service.create(req(Status.CLOSED)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no mitigations");
        verify(riskRepository, never()).save(any());
    }

    @Test
    void allowsCreatingOpenRisk() {
        service.create(req(Status.OPEN));
        verify(riskRepository).save(any(Risk.class));
    }

    @Test
    void getMissingThrowsNotFound() {
        when(riskRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateMissingThrowsNotFound() {
        when(riskRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(99L, req(Status.OPEN)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listSortsByResidualDescending() {
        Risk low = risk(2, 2, 5);   // inherent 4, strong control -> residual 1
        Risk high = risk(5, 5);     // inherent 25, no control -> residual 25
        Risk mid = risk(4, 4, 3);   // inherent 16 -> residual ~8
        when(riskRepository.findAll(Mockito.<org.springframework.data.jpa.domain.Specification<Risk>>any()))
                .thenReturn(List.of(low, high, mid));

        List<RiskResponse> sorted = service.list(null, null, "residualDesc");

        // Sorted by residual descending: high (25) > mid (~8) > low (1).
        assertThat(sorted).extracting(RiskResponse::residualScore)
                .isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(sorted.get(0).residualScore()).isEqualTo(25);
        assertThat(sorted.get(2).residualScore()).isEqualTo(1);
    }

    private Risk risk(int likelihood, int impact, int... mitigationEffectivenesses) {
        Risk r = new Risk("R", "d", Category.OPERATIONAL, "o", likelihood, impact, Status.OPEN, null);
        for (int eff : mitigationEffectivenesses) {
            r.addMitigation(new Mitigation("m", eff));
        }
        return r;
    }
}
