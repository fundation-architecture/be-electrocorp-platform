package com.electrocorp.electrocorpplatform.devicecontrol.domain.services;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DevicePowerPolicyService {

    private static final BigDecimal HIGH_POWER_THRESHOLD = BigDecimal.valueOf(1000);

    public boolean isHighPowerDevice(BigDecimal powerWatts) {
        return powerWatts != null && powerWatts.compareTo(HIGH_POWER_THRESHOLD) >= 0;
    }

    public String resolvePowerCategory(BigDecimal powerWatts) {
        if (powerWatts == null) {
            return "UNKNOWN";
        }

        if (powerWatts.compareTo(BigDecimal.valueOf(100)) < 0) {
            return "LOW";
        }

        if (powerWatts.compareTo(HIGH_POWER_THRESHOLD) < 0) {
            return "MEDIUM";
        }

        return "HIGH";
    }
}
