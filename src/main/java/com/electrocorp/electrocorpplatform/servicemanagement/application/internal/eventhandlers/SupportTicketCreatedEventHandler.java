package com.electrocorp.electrocorpplatform.servicemanagement.application.internal.eventhandlers;

import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.events.SupportTicketCreatedEvent;
import com.electrocorp.electrocorpplatform.servicemanagement.interfaces.events.SupportTicketCreatedIntegrationEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SupportTicketCreatedEventHandler {

    private final IntegrationEventPublisher integrationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(SupportTicketCreatedEvent event) {
        integrationEventPublisher.publish(SupportTicketCreatedIntegrationEvent.from(event));
    }
}
