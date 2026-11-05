package com.electrocorp.electrocorpplatform.energymonitoring.infrastructure.configuration;

import com.electrocorp.electrocorpplatform.energymonitoring.domain.factories.EnergyReadingFactory;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.specifications.HighConsumptionSpecification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Configura el limite de consumo elevado de Energy Monitoring a partir de
 * {@code electrocorp.energy.high-consumption-threshold-watts} (por defecto 1800 W).
 */
@Configuration
public class EnergyMonitoringConfiguration {

    @Bean
    public HighConsumptionSpecification highConsumptionSpecification(
            @Value("${electrocorp.energy.high-consumption-threshold-watts:1800}") BigDecimal thresholdWatts
    ) {
        return new HighConsumptionSpecification(thresholdWatts);
    }

    @Bean
    public EnergyReadingFactory energyReadingFactory(HighConsumptionSpecification highConsumptionSpecification) {
        return new EnergyReadingFactory(highConsumptionSpecification);
    }
}
