package com.hyperproof.riskregister.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * A compliance-framework control a risk can be mapped to (reference data seeded by Flyway).
 * e.g. framework=NIST_CSF, code=PR, name=Protect.
 */
@Entity
@Table(name = "framework_control")
public class FrameworkControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String framework;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    protected FrameworkControl() {
    }

    public Long getId() {
        return id;
    }

    public String getFramework() {
        return framework;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FrameworkControl that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
