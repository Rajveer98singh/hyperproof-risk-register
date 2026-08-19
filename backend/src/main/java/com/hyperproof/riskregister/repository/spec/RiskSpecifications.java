package com.hyperproof.riskregister.repository.spec;

import com.hyperproof.riskregister.domain.Category;
import com.hyperproof.riskregister.domain.Risk;
import com.hyperproof.riskregister.domain.Status;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable JPA specifications for the optional list filters. Each returns a no-op (matches all)
 * when its argument is null, so callers can chain them without null-checking.
 */
public final class RiskSpecifications {

    private RiskSpecifications() {
    }

    public static Specification<Risk> hasCategory(Category category) {
        return (root, query, cb) ->
                category == null ? cb.conjunction() : cb.equal(root.get("category"), category);
    }

    public static Specification<Risk> hasStatus(Status status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }
}
