package com.hyperproof.riskregister.web;

import com.hyperproof.riskregister.repository.FrameworkControlRepository;
import com.hyperproof.riskregister.web.dto.FrameworkResponse;
import com.hyperproof.riskregister.web.mapper.RiskMapper;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the compliance-framework catalog so the UI can offer controls to map risks to. */
@RestController
@RequestMapping("/api/frameworks")
public class FrameworkController {

    private final FrameworkControlRepository repository;
    private final RiskMapper mapper;

    public FrameworkController(FrameworkControlRepository repository, RiskMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @GetMapping
    public List<FrameworkResponse> list() {
        return repository.findAllByOrderByFrameworkAscCodeAsc().stream()
                .map(mapper::toFrameworkResponse)
                .toList();
    }
}
