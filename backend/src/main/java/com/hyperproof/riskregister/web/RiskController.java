package com.hyperproof.riskregister.web;

import com.hyperproof.riskregister.domain.Category;
import com.hyperproof.riskregister.domain.Status;
import com.hyperproof.riskregister.service.RiskService;
import com.hyperproof.riskregister.web.dto.RiskRequest;
import com.hyperproof.riskregister.web.dto.RiskResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risks")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @PostMapping
    public ResponseEntity<RiskResponse> create(@Valid @RequestBody RiskRequest request) {
        RiskResponse body = riskService.create(request);
        return ResponseEntity.created(URI.create("/api/risks/" + body.id())).body(body);
    }

    @GetMapping
    public List<RiskResponse> list(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String sort) {
        return riskService.list(category, status, sort);
    }

    @GetMapping("/{id}")
    public RiskResponse get(@PathVariable Long id) {
        return riskService.get(id);
    }

    @PutMapping("/{id}")
    public RiskResponse update(@PathVariable Long id, @Valid @RequestBody RiskRequest request) {
        return riskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        riskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
