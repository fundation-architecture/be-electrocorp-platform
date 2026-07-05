package com.electrocorp.electrocorpplatform.iam.interfaces.events;

import com.electrocorp.electrocorpplatform.iam.domain.model.events.UserRegisteredEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEvent;

import java.time.LocalDateTime;

public record UserRegisteredIntegrationEvent(
        Long userId,
        String email,
        LocalDateTime occurredOn
) implements IntegrationEvent {
    public static UserRegisteredIntegrationEvent from(UserRegisteredEvent event) {
        return new UserRegisteredIntegrationEvent(event.userId(), event.email(), event.occurredOn());
    }
}
