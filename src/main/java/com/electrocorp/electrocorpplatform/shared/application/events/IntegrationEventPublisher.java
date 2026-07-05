package com.electrocorp.electrocorpplatform.shared.application.events;

public interface IntegrationEventPublisher {
    void publish(IntegrationEvent event);
}
