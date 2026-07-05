package com.electrocorp.electrocorpplatform.devicecontrol.interfaces.events;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.events.DeviceCreatedEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEvent;

import java.time.LocalDateTime;

public record DeviceCreatedIntegrationEvent(
        Long userId,
        Long deviceId,
        String deviceName,
        LocalDateTime occurredOn
) implements IntegrationEvent {
    public static DeviceCreatedIntegrationEvent from(DeviceCreatedEvent event) {
        return new DeviceCreatedIntegrationEvent(
                event.userId(),
                event.deviceId(),
                event.deviceName(),
                event.occurredOn()
        );
    }
}
