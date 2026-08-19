package com.hyperproof.riskregister.domain;

import com.hyperproof.riskregister.security.EncryptedStringConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * An organizational risk in the register.
 *
 * <p>Only raw inputs are persisted (likelihood, impact, mitigations). Inherent and residual scores
 * are always derived on read by {@code ScoringService}, so a stored score can never drift.
 */
@Entity
@Table(name = "risk")
public class Risk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    // owner and description may contain PII / sensitive detail -> encrypted at rest.
    @Convert(converter = EncryptedStringConverter.class)
    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Convert(converter = EncryptedStringConverter.class)
    @Column
    private String owner;

    @Column(nullable = false)
    private int likelihood;

    @Column(nullable = false)
    private int impact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "risk", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Mitigation> mitigations = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "risk_framework",
            joinColumns = @JoinColumn(name = "risk_id"),
            inverseJoinColumns = @JoinColumn(name = "framework_control_id"))
    private Set<FrameworkControl> frameworks = new LinkedHashSet<>();

    protected Risk() {
    }

    public Risk(String title, String description, Category category, String owner,
                int likelihood, int impact, Status status, LocalDate nextReviewDate) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.owner = owner;
        this.likelihood = likelihood;
        this.impact = impact;
        this.status = status;
        this.nextReviewDate = nextReviewDate;
    }

    /** Adds a mitigation and maintains both sides of the relationship. */
    public void addMitigation(Mitigation mitigation) {
        mitigation.setRisk(this);
        this.mitigations.add(mitigation);
    }

    /** Removes a mitigation and clears the back-reference (orphanRemoval deletes the row). */
    public void removeMitigation(Mitigation mitigation) {
        this.mitigations.remove(mitigation);
        mitigation.setRisk(null);
    }

    // --- getters / setters (raw inputs only; scores are computed elsewhere) ---

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getLikelihood() {
        return likelihood;
    }

    public void setLikelihood(int likelihood) {
        this.likelihood = likelihood;
    }

    public int getImpact() {
        return impact;
    }

    public void setImpact(int impact) {
        this.impact = impact;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Mitigation> getMitigations() {
        return mitigations;
    }

    public Set<FrameworkControl> getFrameworks() {
        return frameworks;
    }

    public void setFrameworks(Set<FrameworkControl> frameworks) {
        this.frameworks = frameworks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Risk that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
