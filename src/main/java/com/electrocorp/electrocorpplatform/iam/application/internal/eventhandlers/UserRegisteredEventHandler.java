package com.electrocorp.electrocorpplatform.iam.application.internal.eventhandlers;

import com.electrocorp.electrocorpplatform.iam.domain.model.events.UserRegisteredEvent;
import com.electrocorp.electrocorpplatform.iam.interfaces.events.UserRegisteredIntegrationEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserRegisteredEventHandler {

    private final IntegrationEventPublisher integrationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(UserRegisteredEvent event) {
        integrationEventPublisher.publish(UserRegisteredIntegrationEvent.from(event));
    }
}
