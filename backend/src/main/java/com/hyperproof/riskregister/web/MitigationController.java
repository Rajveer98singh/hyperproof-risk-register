package com.hyperproof.riskregister.web;

import com.hyperproof.riskregister.service.MitigationService;
import com.hyperproof.riskregister.web.dto.MitigationRequest;
import com.hyperproof.riskregister.web.dto.RiskResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mitigation endpoints. Mutations return the parent {@link RiskResponse} so the client immediately
 * sees the recomputed residual score.
 */
@RestController
public class MitigationController {

    private final MitigationService mitigationService;

    public MitigationController(MitigationService mitigationService) {
        this.mitigationService = mitigationService;
    }

    @PostMapping("/api/risks/{riskId}/mitigations")
    @ResponseStatus(HttpStatus.CREATED)
    public RiskResponse add(@PathVariable Long riskId, @Valid @RequestBody MitigationRequest request) {
        return mitigationService.addMitigation(riskId, request.description(), request.effectiveness());
    }

    @PutMapping("/api/mitigations/{id}")
    public RiskResponse update(@PathVariable Long id, @Valid @RequestBody MitigationRequest request) {
        return mitigationService.updateMitigation(id, request.description(), request.effectiveness());
    }

    @DeleteMapping("/api/mitigations/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mitigationService.deleteMitigation(id);
        return ResponseEntity.noContent().build();
    }
}
