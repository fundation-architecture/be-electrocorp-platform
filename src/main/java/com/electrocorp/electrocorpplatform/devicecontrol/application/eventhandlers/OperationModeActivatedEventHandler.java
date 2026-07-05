package com.electrocorp.electrocorpplatform.devicecontrol.application.eventhandlers;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.events.OperationModeActivatedEvent;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.events.OperationModeActivatedIntegrationEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OperationModeActivatedEventHandler {

    private final IntegrationEventPublisher integrationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(OperationModeActivatedEvent event) {
        integrationEventPublisher.publish(OperationModeActivatedIntegrationEvent.from(event));
    }
}
