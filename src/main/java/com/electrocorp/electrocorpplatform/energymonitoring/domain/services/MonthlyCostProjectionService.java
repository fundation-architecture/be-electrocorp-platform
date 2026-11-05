package com.electrocorp.electrocorpplatform.energymonitoring.domain.services;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Proyecta el gasto del mes (US-18):
 * proyeccion = costo acumulado del mes / dias transcurridos * dias del mes.
 * Ejemplo: S/ 20.00 acumulados en 10 dias de un mes de 30 dias proyectan S/ 60.00.
 */
@Service
public class MonthlyCostProjectionService {

    public BigDecimal project(BigDecimal accumulatedMonthCost, LocalDate today) {
        return project(accumulatedMonthCost, today.getDayOfMonth(), today.lengthOfMonth());
    }

    public BigDecimal project(BigDecimal accumulatedMonthCost, int daysElapsed, int daysInMonth) {
        if (accumulatedMonthCost == null || accumulatedMonthCost.signum() <= 0 || daysElapsed <= 0 || daysInMonth <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        int elapsed = Math.min(daysElapsed, daysInMonth);
        return accumulatedMonthCost
                .multiply(BigDecimal.valueOf(daysInMonth))
                .divide(BigDecimal.valueOf(elapsed), 2, RoundingMode.HALF_UP);
    }
}
