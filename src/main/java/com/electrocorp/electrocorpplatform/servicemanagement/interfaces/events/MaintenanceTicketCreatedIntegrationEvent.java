package com.electrocorp.electrocorpplatform.servicemanagement.interfaces.events;

import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.events.MaintenanceTicketCreatedEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEvent;

import java.time.LocalDateTime;

public record MaintenanceTicketCreatedIntegrationEvent(
        Long userId,
        Long ticketId,
        Long deviceId,
        LocalDateTime occurredOn
) implements IntegrationEvent {
    public static MaintenanceTicketCreatedIntegrationEvent from(MaintenanceTicketCreatedEvent event) {
        return new MaintenanceTicketCreatedIntegrationEvent(
                event.userId(),
                event.ticketId(),
                event.deviceId(),
                event.occurredOn()
        );
    }
}
