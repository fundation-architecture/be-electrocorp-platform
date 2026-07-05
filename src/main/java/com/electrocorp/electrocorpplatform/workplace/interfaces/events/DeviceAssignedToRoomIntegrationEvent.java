package com.electrocorp.electrocorpplatform.workplace.interfaces.events;

import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEvent;
import com.electrocorp.electrocorpplatform.workplace.domain.model.events.DeviceAssignedToRoomEvent;

import java.time.LocalDateTime;

public record DeviceAssignedToRoomIntegrationEvent(
        Long userId,
        Long deviceId,
        Long roomId,
        LocalDateTime occurredOn
) implements IntegrationEvent {
    public static DeviceAssignedToRoomIntegrationEvent from(DeviceAssignedToRoomEvent event) {
        return new DeviceAssignedToRoomIntegrationEvent(
                event.userId(),
                event.deviceId(),
                event.roomId(),
                event.occurredOn()
        );
    }
}
