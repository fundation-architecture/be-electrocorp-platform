package com.electrocorp.electrocorpplatform.iam.application.internal.eventhandlers;

import com.electrocorp.electrocorpplatform.iam.domain.model.events.UserRegisteredEvent;
import com.electrocorp.electrocorpplatform.iam.interfaces.events.UserRegisteredIntegrationEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEventPublisher;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserRegisteredEventHandlerTest {

    @Test
    void translatesDomainEventIntoIntegrationEvent() {
        IntegrationEventPublisher publisher = mock(IntegrationEventPublisher.class);
        UserRegisteredEventHandler handler = new UserRegisteredEventHandler(publisher);

        handler.on(new UserRegisteredEvent(42L, "member@electrocorp.pe"));

        verify(publisher).publish(isA(UserRegisteredIntegrationEvent.class));
    }
}
