package com.electrocorp.electrocorpplatform.billing.interfaces.events;

import com.electrocorp.electrocorpplatform.billing.domain.model.events.SubscriptionActivatedEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEvent;

import java.time.LocalDateTime;

public record SubscriptionActivatedIntegrationEvent(
        Long userId,
        Long subscriptionId,
        String planCode,
        LocalDateTime occurredOn
) implements IntegrationEvent {
    public static SubscriptionActivatedIntegrationEvent from(SubscriptionActivatedEvent event) {
        return new SubscriptionActivatedIntegrationEvent(
                event.userId(),
                event.subscriptionId(),
                event.planCode(),
                event.occurredOn()
        );
    }
}
