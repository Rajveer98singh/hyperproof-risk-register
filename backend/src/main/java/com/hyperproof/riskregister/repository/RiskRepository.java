package com.hyperproof.riskregister.repository;

import com.hyperproof.riskregister.domain.Risk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * {@link JpaSpecificationExecutor} lets us compose optional category/status filters cleanly via
 * {@link com.hyperproof.riskregister.repository.spec.RiskSpecifications}.
 */
public interface RiskRepository extends JpaRepository<Risk, Long>, JpaSpecificationExecutor<Risk> {
}
