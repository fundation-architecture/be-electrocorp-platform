package com.electrocorp.electrocorpplatform.shared.infrastructure.events;

import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringIntegrationEventPublisher implements IntegrationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(IntegrationEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
