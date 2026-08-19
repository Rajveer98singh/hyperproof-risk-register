package com.hyperproof.riskregister.domain;

import com.hyperproof.riskregister.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;

/** A control applied to a {@link Risk}. Its effectiveness feeds the residual-score calculation. */
@Entity
@Table(name = "mitigation")
public class Mitigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "risk_id", nullable = false)
    private Risk risk;

    // Encrypted at rest (may contain sensitive control detail).
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private int effectiveness;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Mitigation() {
    }

    public Mitigation(String description, int effectiveness) {
        this.description = description;
        this.effectiveness = effectiveness;
    }

    public Long getId() {
        return id;
    }

    public Risk getRisk() {
        return risk;
    }

    void setRisk(Risk risk) {
        this.risk = risk;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getEffectiveness() {
        return effectiveness;
    }

    public void setEffectiveness(int effectiveness) {
        this.effectiveness = effectiveness;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mitigation that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
