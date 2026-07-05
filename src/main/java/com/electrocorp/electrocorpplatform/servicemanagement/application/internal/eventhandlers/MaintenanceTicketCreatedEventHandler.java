package com.electrocorp.electrocorpplatform.servicemanagement.application.internal.eventhandlers;

import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.events.MaintenanceTicketCreatedEvent;
import com.electrocorp.electrocorpplatform.servicemanagement.interfaces.events.MaintenanceTicketCreatedIntegrationEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MaintenanceTicketCreatedEventHandler {

    private final IntegrationEventPublisher integrationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(MaintenanceTicketCreatedEvent event) {
        integrationEventPublisher.publish(MaintenanceTicketCreatedIntegrationEvent.from(event));
    }
}
