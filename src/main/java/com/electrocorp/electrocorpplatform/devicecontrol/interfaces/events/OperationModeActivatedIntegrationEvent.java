package com.electrocorp.electrocorpplatform.devicecontrol.interfaces.events;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.events.OperationModeActivatedEvent;
import com.electrocorp.electrocorpplatform.shared.application.events.IntegrationEvent;

import java.time.LocalDateTime;

public record OperationModeActivatedIntegrationEvent(
        Long userId,
        Long modeId,
        String modeName,
        String evidence,
        String explanation,
        String recommendedAction,
        LocalDateTime occurredOn
) implements IntegrationEvent {
    public static OperationModeActivatedIntegrationEvent from(OperationModeActivatedEvent event) {
        return new OperationModeActivatedIntegrationEvent(
                event.userId(),
                event.modeId(),
                event.modeName(),
                event.evidence(),
                event.explanation(),
                event.recommendedAction(),
                event.occurredOn()
        );
    }
}
