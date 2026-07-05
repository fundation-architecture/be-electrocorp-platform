package com.electrocorp.electrocorpplatform.billing.interfaces.events;

import com.electrocorp.electrocorpplatform.billing.domain.model.events.PaymentRegisteredEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRegisteredIntegrationEvent(
        Long userId,
        Long paymentId,
        BigDecimal amount,
        LocalDateTime occurredOn
) implements IntegrationEvent {
    public static PaymentRegisteredIntegrationEvent from(PaymentRegisteredEvent event) {
        return new PaymentRegisteredIntegrationEvent(
                event.userId(),
                event.paymentId(),
                event.amount(),
                event.occurredOn()
        );
    }
}
