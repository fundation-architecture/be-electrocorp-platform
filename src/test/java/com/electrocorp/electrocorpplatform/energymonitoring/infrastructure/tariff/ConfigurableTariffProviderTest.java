package com.electrocorp.electrocorpplatform.energymonitoring.infrastructure.tariff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurableTariffProviderTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ConfigurableTariffProvider.class);

    @Test
    void usesTheDefaultTariffWhenThePropertyIsMissing() {
        contextRunner.run(context -> assertThat(context.getBean(ConfigurableTariffProvider.class).tariffPerKilowattHour())
                .isEqualByComparingTo("0.75"));
    }

    @Test
    void readsTheTariffFromConfiguration() {
        contextRunner.withPropertyValues("electrocorp.energy.tariff-per-kwh=0.82")
                .run(context -> assertThat(context.getBean(ConfigurableTariffProvider.class).tariffPerKilowattHour())
                        .isEqualByComparingTo("0.82"));
    }

    @Test
    void rejectsANegativeTariff() {
        assertThatThrownBy(() -> new ConfigurableTariffProvider(BigDecimal.valueOf(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
