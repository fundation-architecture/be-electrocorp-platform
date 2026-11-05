package com.electrocorp.electrocorpplatform.energymonitoring.domain.services;

import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Calcula los indicadores de consumo actual del panel (US-13).
 * <ul>
 *     <li>currentWatts: suma de la ultima lectura del dia de cada dispositivo encendido.
 *     Un dispositivo encendido que todavia no envia lecturas aporta 0 W.</li>
 *     <li>todayKilowattHours: suma de los kWh registrados hoy por los dispositivos del usuario.</li>
 * </ul>
 */
@Service
public class CurrentConsumptionService {

    public BigDecimal calculateCurrentWatts(Collection<Long> activeDeviceIds, List<EnergyReading> todayReadings) {
        if (activeDeviceIds == null || activeDeviceIds.isEmpty() || todayReadings == null || todayReadings.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return activeDeviceIds.stream()
                .map(deviceId -> latestWattsOf(deviceId, todayReadings))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTodayKilowattHours(List<EnergyReading> todayReadings) {
        if (todayReadings == null || todayReadings.isEmpty()) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }

        return todayReadings.stream()
                .map(EnergyReading::getKilowattHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal latestWattsOf(Long deviceId, List<EnergyReading> readings) {
        return readings.stream()
                .filter(reading -> deviceId.equals(reading.getDeviceId()))
                .filter(reading -> reading.getRecordedAt() != null && reading.getWatts() != null)
                .max(Comparator.comparing(EnergyReading::getRecordedAt))
                .map(EnergyReading::getWatts)
                .orElse(BigDecimal.ZERO);
    }
}
