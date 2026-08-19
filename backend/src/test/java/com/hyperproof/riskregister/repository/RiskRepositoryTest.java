package com.hyperproof.riskregister.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hyperproof.riskregister.domain.Category;
import com.hyperproof.riskregister.domain.Mitigation;
import com.hyperproof.riskregister.domain.Risk;
import com.hyperproof.riskregister.domain.Status;
import com.hyperproof.riskregister.repository.spec.RiskSpecifications;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * Repository slice test against Flyway-migrated H2. Verifies schema constraints, cascade behavior,
 * specification filtering, and that encrypted columns are stored as ciphertext.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RiskRepositoryTest {

    @Autowired
    private RiskRepository riskRepository;

    @Autowired
    private TestEntityManager em;

    private Risk newRisk(String title, Category category, Status status, int likelihood, int impact) {
        return new Risk(title, "desc-" + title, category, "owner-" + title,
                likelihood, impact, status, null);
    }

    @Test
    void savesAndCascadesMitigations() {
        Risk risk = newRisk("Cascade", Category.SECURITY, Status.MITIGATING, 4, 4);
        risk.addMitigation(new Mitigation("MFA", 5));
        risk.addMitigation(new Mitigation("Training", 2));
        Risk saved = riskRepository.saveAndFlush(risk);
        em.clear();

        Risk found = riskRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getMitigations()).hasSize(2);
    }

    @Test
    void orphanRemovalDeletesMitigationRows() {
        Risk risk = newRisk("Orphan", Category.OPERATIONAL, Status.MITIGATING, 3, 3);
        risk.addMitigation(new Mitigation("Backups", 4));
        Risk saved = riskRepository.saveAndFlush(risk);

        saved.getMitigations().clear();
        riskRepository.saveAndFlush(saved);
        em.clear();

        Long count = em.getEntityManager()
                .createQuery("select count(m) from Mitigation m", Long.class)
                .getSingleResult();
        assertThat(count).isZero();
    }

    @Test
    void checkConstraintRejectsOutOfRangeLikelihood() {
        Risk bad = newRisk("BadLikelihood", Category.FINANCIAL, Status.OPEN, 0, 3);
        assertThatThrownBy(() -> riskRepository.saveAndFlush(bad))
                .isInstanceOf(Exception.class); // DB CHECK (likelihood BETWEEN 1 AND 5)
    }

    @Test
    void checkConstraintRejectsOutOfRangeEffectiveness() {
        Risk risk = newRisk("BadEff", Category.SECURITY, Status.MITIGATING, 3, 3);
        risk.addMitigation(new Mitigation("Overpowered", 6));
        assertThatThrownBy(() -> riskRepository.saveAndFlush(risk))
                .isInstanceOf(Exception.class);
    }

    @Test
    void specificationFiltersByCategoryAndStatus() {
        riskRepository.saveAndFlush(newRisk("A", Category.SECURITY, Status.OPEN, 2, 2));
        riskRepository.saveAndFlush(newRisk("B", Category.SECURITY, Status.CLOSED, 2, 2));
        riskRepository.saveAndFlush(newRisk("C", Category.FINANCIAL, Status.OPEN, 2, 2));

        List<Risk> securityOpen = riskRepository.findAll(
                RiskSpecifications.hasCategory(Category.SECURITY)
                        .and(RiskSpecifications.hasStatus(Status.OPEN)));

        assertThat(securityOpen).extracting(Risk::getTitle).containsExactly("A");
    }

    @Test
    void ownerColumnIsStoredEncrypted() {
        Risk risk = newRisk("Secret", Category.COMPLIANCE, Status.OPEN, 2, 2);
        Risk saved = riskRepository.saveAndFlush(risk);
        em.clear();

        // Read the raw column value directly, bypassing the JPA converter.
        Object raw = em.getEntityManager()
                .createNativeQuery("select owner from risk where id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();

        assertThat(raw.toString()).doesNotContain("owner-Secret");
        // And the entity round-trips back to plaintext through the converter.
        assertThat(riskRepository.findById(saved.getId()).orElseThrow().getOwner())
                .isEqualTo("owner-Secret");
    }
}
