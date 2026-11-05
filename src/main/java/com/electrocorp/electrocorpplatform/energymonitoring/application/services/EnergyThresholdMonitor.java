package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.EnergyReadingStatus;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.events.EnergyThresholdExceededEvent;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.specifications.HighConsumptionSpecification;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Unico punto que publica {@link EnergyThresholdExceededEvent}: lo hace una vez por cada lectura
 * guardada que supera el limite configurado, tanto en la ingesta manual como en la del planificador.
 */
@Component
@RequiredArgsConstructor
public class EnergyThresholdMonitor {

    private final DomainEventPublisher domainEventPublisher;
    private final HighConsumptionSpecification highConsumptionSpecification;

    public boolean publishIfExceeded(EnergyReading reading) {
        if (reading == null || reading.getWatts() == null || reading.getStatus() != EnergyReadingStatus.HIGH) {
            return false;
        }

        domainEventPublisher.publish(new EnergyThresholdExceededEvent(
                reading.getUserId(),
                reading.getDeviceId(),
                reading.getDeviceName(),
                reading.getWatts(),
                highConsumptionSpecification.thresholdWatts()
        ));
        return true;
    }
}
