package com.electrocorp.electrocorpplatform.energymonitoring.domain.services;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class EnergyConsumptionSimulatorService {

    public BigDecimal simulateWatts(BigDecimal ratedPowerWatts) {
        if (ratedPowerWatts == null || ratedPowerWatts.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return ratedPowerWatts.setScale(2, RoundingMode.HALF_UP);
    }
}
