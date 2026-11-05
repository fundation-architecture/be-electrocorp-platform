package com.electrocorp.electrocorpplatform.energymonitoring.application.ports;

import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.EnergyDashboardSummaryResource;

import java.util.Optional;

/**
 * Puerto de cache del resumen de consumo (QA-02). La clave sigue el formato
 * {@code consumption:{id}:summary} definido en el Capitulo IV y vive 30 s.
 */
public interface ConsumptionSummaryCache {

    String KEY_PATTERN = "consumption:%d:summary";

    Optional<EnergyDashboardSummaryResource> get(Long userId);

    void put(Long userId, EnergyDashboardSummaryResource summary);

    void evict(Long userId);

    static String keyFor(Long userId) {
        return KEY_PATTERN.formatted(userId);
    }
}
