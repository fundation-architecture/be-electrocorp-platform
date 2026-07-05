package com.electrocorp.electrocorpplatform.energymonitoring.application.internal.eventhandlers;

import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.events.EnergyThresholdExceededEvent;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.events.EnergyThresholdExceededIntegrationEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EnergyThresholdExceededEventHandler {

    private final IntegrationEventPublisher integrationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(EnergyThresholdExceededEvent event) {
        integrationEventPublisher.publish(EnergyThresholdExceededIntegrationEvent.from(event));
    }
}
