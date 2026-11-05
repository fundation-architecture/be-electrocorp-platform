package com.electrocorp.electrocorpplatform.energymonitoring.infrastructure.cache;

import com.electrocorp.electrocorpplatform.energymonitoring.application.ports.ConsumptionSummaryCache;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.EnergyDashboardSummaryResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador sin cache, usado cuando electrocorp.energy.summary-cache.enabled=false
 * (por ejemplo, en el perfil de pruebas).
 */
@Component
@ConditionalOnProperty(name = "electrocorp.energy.summary-cache.enabled", havingValue = "false")
public class NoOpConsumptionSummaryCache implements ConsumptionSummaryCache {

    @Override
    public Optional<EnergyDashboardSummaryResource> get(Long userId) {
        return Optional.empty();
    }

    @Override
    public void put(Long userId, EnergyDashboardSummaryResource summary) {
        // Sin cache: no se guarda nada.
    }

    @Override
    public void evict(Long userId) {
        // Sin cache: no hay nada que invalidar.
    }
}
