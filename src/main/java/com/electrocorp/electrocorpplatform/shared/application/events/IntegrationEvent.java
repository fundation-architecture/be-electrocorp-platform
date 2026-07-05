package com.electrocorp.electrocorpplatform.shared.application.events;

import java.time.LocalDateTime;

public interface IntegrationEvent {
    LocalDateTime occurredOn();
}
