package com.electrocorp.electrocorpplatform.energymonitoring.interfaces.events;

import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.events.EnergyThresholdExceededEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EnergyThresholdExceededIntegrationEvent(
        Long userId,
        Long deviceId,
        String deviceName,
        BigDecimal watts,
        BigDecimal thresholdWatts,
        LocalDateTime occurredOn
) implements IntegrationEvent {
    public static EnergyThresholdExceededIntegrationEvent from(EnergyThresholdExceededEvent event) {
        return new EnergyThresholdExceededIntegrationEvent(
                event.userId(),
                event.deviceId(),
                event.deviceName(),
                event.watts(),
                event.thresholdWatts(),
                event.occurredOn()
        );
    }
}
