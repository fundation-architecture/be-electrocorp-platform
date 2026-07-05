package com.electrocorp.electrocorpplatform.reporting.application.internal.eventhandlers;

import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.events.EnergyThresholdExceededIntegrationEvent;
import com.electrocorp.electrocorpplatform.reporting.application.commandservices.ReportingCommandService;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.RecordReportingEventCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReportingIntegrationEventHandlerTest {

    @Test
    void recordsEnergyThresholdIntegrationEventForReporting() {
        ReportingCommandService commandService = mock(ReportingCommandService.class);
        ReportingIntegrationEventHandler handler = new ReportingIntegrationEventHandler(commandService);
        LocalDateTime occurredOn = LocalDateTime.of(2026, 6, 30, 14, 15);

        handler.on(new EnergyThresholdExceededIntegrationEvent(
                9L,
                33L,
                BigDecimal.valueOf(1850),
                occurredOn
        ));

        verify(commandService).handle(argThat((RecordReportingEventCommand command) ->
                command.userId().equals(9L)
                        && command.eventName().equals("ENERGY_THRESHOLD_EXCEEDED")
                        && command.sourceContext().equals("ENERGY_MONITORING")
                        && command.subjectType().equals("DEVICE")
                        && command.subjectId().equals("33")
                        && command.occurredOn().equals(occurredOn)
        ));
    }
}
