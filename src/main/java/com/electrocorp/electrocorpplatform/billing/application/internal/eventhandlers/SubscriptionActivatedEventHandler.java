package com.electrocorp.electrocorpplatform.billing.application.internal.eventhandlers;

import com.electrocorp.electrocorpplatform.billing.domain.model.events.SubscriptionActivatedEvent;
import com.electrocorp.electrocorpplatform.billing.interfaces.events.SubscriptionActivatedIntegrationEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SubscriptionActivatedEventHandler {

    private final IntegrationEventPublisher integrationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(SubscriptionActivatedEvent event) {
        integrationEventPublisher.publish(SubscriptionActivatedIntegrationEvent.from(event));
    }
}
