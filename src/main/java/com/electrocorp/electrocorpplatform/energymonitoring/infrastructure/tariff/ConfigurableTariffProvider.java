package com.electrocorp.electrocorpplatform.energymonitoring.infrastructure.tariff;

import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.TariffProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adaptador de {@link TariffProvider} que toma la tarifa de la propiedad
 * {@code electrocorp.energy.tariff-per-kwh} (valor por defecto 0.75 S/ por kWh).
 */
@Component
public class ConfigurableTariffProvider implements TariffProvider {

    private final BigDecimal tariffPerKilowattHour;

    public ConfigurableTariffProvider(@Value("${electrocorp.energy.tariff-per-kwh:0.75}") BigDecimal tariffPerKilowattHour) {
        if (tariffPerKilowattHour == null || tariffPerKilowattHour.signum() < 0) {
            throw new IllegalArgumentException("electrocorp.energy.tariff-per-kwh must be zero or positive.");
        }
        this.tariffPerKilowattHour = tariffPerKilowattHour;
    }

    @Override
    public BigDecimal tariffPerKilowattHour() {
        return tariffPerKilowattHour;
    }
}
