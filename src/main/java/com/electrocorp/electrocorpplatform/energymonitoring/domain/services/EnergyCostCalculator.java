package com.electrocorp.electrocorpplatform.energymonitoring.domain.services;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula los costos de energia en soles (S/) con la tarifa que entrega {@link TariffProvider} (US-16).
 */
@Service
public class EnergyCostCalculator {

    public static final String CURRENCY = "PEN";
    public static final String CURRENCY_SYMBOL = "S/";

    private static final BigDecimal WATTS_PER_KILOWATT = BigDecimal.valueOf(1000);
    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);

    private final TariffProvider tariffProvider;

    public EnergyCostCalculator(TariffProvider tariffProvider) {
        this.tariffProvider = tariffProvider;
    }

    /** Costo en soles de los kWh consumidos, redondeado a centimos. */
    public BigDecimal costOf(BigDecimal kilowattHours) {
        return preciseCostOf(kilowattHours).setScale(2, RoundingMode.HALF_UP);
    }

    /** Costo en soles con 6 decimales, para guardar el costo de cada lectura sin perder precision. */
    public BigDecimal preciseCostOf(BigDecimal kilowattHours) {
        if (kilowattHours == null || kilowattHours.signum() <= 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }

        return kilowattHours
                .multiply(tariffProvider.tariffPerKilowattHour())
                .setScale(6, RoundingMode.HALF_UP);
    }

    /** Costo en soles por hora si la potencia indicada se mantiene durante una hora. */
    public BigDecimal costPerHour(BigDecimal watts) {
        if (watts == null || watts.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return costOf(watts.divide(WATTS_PER_KILOWATT, 6, RoundingMode.HALF_UP));
    }

    /** kWh consumidos por una potencia constante durante los segundos indicados. */
    public BigDecimal kilowattHoursOf(BigDecimal watts, int seconds) {
        if (watts == null || watts.signum() <= 0 || seconds <= 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }

        return watts
                .divide(WATTS_PER_KILOWATT, 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(seconds))
                .divide(SECONDS_PER_HOUR, 6, RoundingMode.HALF_UP);
    }
}
