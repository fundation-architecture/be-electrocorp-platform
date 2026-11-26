package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.EnergyReadingStatus;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.events.EnergyThresholdExceededEvent;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.specifications.HighConsumptionSpecification;
import com.electrocorp.electrocorpplatform.notifications.application.facades.NotificationsContextFacade;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Evalua cada lectura contra el limite de consumo y publica {@link EnergyThresholdExceededEvent}
 * una sola vez por lectura que lo supera. El limite efectivo es el menor entre el limite general de
 * la plataforma (electrocorp.energy.high-consumption-threshold-watts) y el limite que el usuario
 * configuro para el dispositivo (US-19), que se consulta en Notifications por su fachada.
 */
@Component
public class EnergyThresholdMonitor {

    private final DomainEventPublisher domainEventPublisher;
    private final HighConsumptionSpecification highConsumptionSpecification;
    private final NotificationsContextFacade notificationsContextFacade;

    public EnergyThresholdMonitor(
            DomainEventPublisher domainEventPublisher,
            HighConsumptionSpecification highConsumptionSpecification,
            NotificationsContextFacade notificationsContextFacade
    ) {
        this.domainEventPublisher = domainEventPublisher;
        this.highConsumptionSpecification = highConsumptionSpecification;
        this.notificationsContextFacade = notificationsContextFacade;
    }

    /** Marca la lectura como HIGH si supera el limite que el usuario configuro para el dispositivo. */
    public void applyUserLimit(EnergyReading reading) {
        if (reading == null || reading.getWatts() == null || reading.getStatus() == EnergyReadingStatus.HIGH) {
            return;
        }

        userLimit(reading)
                .filter(limit -> reading.getWatts().compareTo(limit) > 0)
                .ifPresent(limit -> reading.setStatus(EnergyReadingStatus.HIGH));
    }

    public boolean publishIfExceeded(EnergyReading reading) {
        if (reading == null || reading.getWatts() == null || reading.getStatus() != EnergyReadingStatus.HIGH) {
            return false;
        }

        domainEventPublisher.publish(new EnergyThresholdExceededEvent(
                reading.getUserId(),
                reading.getDeviceId(),
                reading.getDeviceName(),
                reading.getWatts(),
                effectiveLimit(reading)
        ));
        return true;
    }

    private BigDecimal effectiveLimit(EnergyReading reading) {
        BigDecimal platformLimit = highConsumptionSpecification.thresholdWatts();
        return userLimit(reading)
                .filter(limit -> limit.compareTo(platformLimit) < 0)
                .orElse(platformLimit);
    }

    private Optional<BigDecimal> userLimit(EnergyReading reading) {
        return notificationsContextFacade.findConsumptionLimitWatts(reading.getUserId(), reading.getDeviceId());
    }
}
