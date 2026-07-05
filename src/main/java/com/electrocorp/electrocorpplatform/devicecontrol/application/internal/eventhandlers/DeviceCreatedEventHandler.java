package com.electrocorp.electrocorpplatform.devicecontrol.application.internal.eventhandlers;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.events.DeviceCreatedEvent;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.events.DeviceCreatedIntegrationEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DeviceCreatedEventHandler {

    private final IntegrationEventPublisher integrationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(DeviceCreatedEvent event) {
        integrationEventPublisher.publish(DeviceCreatedIntegrationEvent.from(event));
    }
}
