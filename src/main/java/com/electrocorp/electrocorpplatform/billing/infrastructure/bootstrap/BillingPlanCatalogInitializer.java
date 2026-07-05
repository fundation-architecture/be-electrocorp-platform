package com.electrocorp.electrocorpplatform.billing.infrastructure.bootstrap;

import com.electrocorp.electrocorpplatform.billing.domain.model.aggregates.Plan;
import com.electrocorp.electrocorpplatform.billing.infrastructure.persistence.jpa.repositories.JpaPlanRepository;
import com.electrocorp.electrocorpplatform.shared.domain.valueobjects.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile({"dev", "prod", "test"})
@RequiredArgsConstructor
public class BillingPlanCatalogInitializer implements ApplicationRunner {

    private final JpaPlanRepository planRepository;

    @Override
    public void run(ApplicationArguments args) {
        defaultPlans().stream()
                .filter(plan -> planRepository.findByCode(plan.getCode()).isEmpty())
                .forEach(planRepository::save);
    }

    private List<Plan> defaultPlans() {
        return List.of(
                new Plan(
                        "STARTER",
                        "Starter",
                        Money.of(new BigDecimal("29.00"), "PEN"),
                        5,
                        3,
                        5,
                        false
                ),
                new Plan(
                        "PROFESSIONAL",
                        "Professional",
                        Money.of(new BigDecimal("79.00"), "PEN"),
                        20,
                        15,
                        30,
                        true
                ),
                new Plan(
                        "ENTERPRISE",
                        "Enterprise",
                        Money.of(new BigDecimal("199.00"), "PEN"),
                        100,
                        100,
                        9999,
                        true
                )
        );
    }
}
