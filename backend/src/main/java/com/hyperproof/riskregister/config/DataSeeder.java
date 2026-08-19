package com.hyperproof.riskregister.config;

import com.hyperproof.riskregister.domain.Category;
import com.hyperproof.riskregister.domain.FrameworkControl;
import com.hyperproof.riskregister.domain.Mitigation;
import com.hyperproof.riskregister.domain.Risk;
import com.hyperproof.riskregister.domain.Status;
import com.hyperproof.riskregister.repository.FrameworkControlRepository;
import com.hyperproof.riskregister.repository.RiskRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds a handful of varied risks (dev profile only) so the dashboard is populated on first run —
 * spanning categories, severities, mitigation counts, review dates (one overdue), and framework
 * mappings. Never runs under the {@code test} profile, keeping integration tests deterministic.
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final RiskRepository riskRepository;
    private final FrameworkControlRepository frameworkRepository;

    public DataSeeder(RiskRepository riskRepository, FrameworkControlRepository frameworkRepository) {
        this.riskRepository = riskRepository;
        this.frameworkRepository = frameworkRepository;
    }

    @Override
    public void run(String... args) {
        if (riskRepository.count() > 0) {
            return; // idempotent
        }

        FrameworkControl protect = framework("NIST_CSF", "PR");
        FrameworkControl identify = framework("NIST_CSF", "ID");
        FrameworkControl soc2Security = framework("SOC2", "CC");
        FrameworkControl privacy = framework("SOC2", "P");

        // Critical, unmitigated -> residual == inherent (25).
        Risk breach = risk("Customer data breach via exposed API", Category.SECURITY, Status.OPEN,
                5, 5, "Priya (Security Lead)", LocalDate.now().minusDays(3)); // overdue
        breach.setFrameworks(Set.of(protect, soc2Security, privacy));

        // Strong control -> residual drops sharply.
        Risk vendor = risk("Critical vendor outage", Category.OPERATIONAL, Status.MITIGATING,
                4, 5, "Marcus (Ops)", LocalDate.now().plusMonths(2));
        vendor.addMitigation(new Mitigation("Multi-region failover with automated cutover", 5));
        vendor.setFrameworks(Set.of(identify));

        // Multiple mitigations of mixed strength (max effectiveness wins).
        Risk fraud = risk("Payment fraud", Category.FINANCIAL, Status.MITIGATING,
                4, 4, "Dana (Finance)", LocalDate.now().plusWeeks(3));
        fraud.addMitigation(new Mitigation("Manual review of large transactions", 2));
        fraud.addMitigation(new Mitigation("ML-based real-time fraud scoring", 4));

        // Closed risk (has a mitigation, so allowed to be Closed).
        Risk gdpr = risk("GDPR records-of-processing gap", Category.COMPLIANCE, Status.CLOSED,
                3, 4, "Lena (Compliance)", LocalDate.now().plusMonths(6));
        gdpr.addMitigation(new Mitigation("Documented RoPA reviewed quarterly", 4));
        gdpr.setFrameworks(Set.of(privacy));

        // Low inherent, no mitigation.
        Risk roadmap = risk("Roadmap misalignment with market", Category.STRATEGIC, Status.OPEN,
                2, 3, "Sam (Product)", null);

        // Weak control only -> small reduction.
        Risk insider = risk("Insider misuse of admin access", Category.SECURITY, Status.MITIGATING,
                3, 5, "Priya (Security Lead)", LocalDate.now().plusDays(10));
        insider.addMitigation(new Mitigation("Quarterly access review", 2));
        insider.setFrameworks(Set.of(protect, identify));

        riskRepository.saveAll(List.of(breach, vendor, fraud, gdpr, roadmap, insider));
    }

    private Risk risk(String title, Category category, Status status, int likelihood, int impact,
                      String owner, LocalDate nextReview) {
        return new Risk(title, title + " — details.", category, owner, likelihood, impact, status, nextReview);
    }

    private FrameworkControl framework(String framework, String code) {
        return frameworkRepository.findAll().stream()
                .filter(fc -> fc.getFramework().equals(framework) && fc.getCode().equals(code))
                .findFirst()
                .orElseThrow();
    }
}
