package com.electrocorp.electrocorpplatform.notifications.application.internal.eventhandlers;

import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.events.EnergyThresholdExceededIntegrationEvent;
import com.electrocorp.electrocorpplatform.notifications.application.services.NotificationApplicationService;
import com.electrocorp.electrocorpplatform.notifications.domain.model.commands.CreateAlertCommand;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.RuleScopeType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-36: Notifications crea la alerta de consumo elevado a partir del integration event,
 * con el usuario y el dispositivo involucrado.
 */
class HighConsumptionAlertHandlerTest {

    private final NotificationApplicationService notificationService = mock(NotificationApplicationService.class);
    private final NotificationIntegrationEventHandler handler = new NotificationIntegrationEventHandler(notificationService);

    @Test
    void createsTheHighConsumptionAlertForTheUserAndDevice() {
        when(notificationService.createAlertFromRuleEvaluation(any(), any(), any(), any(), any())).thenReturn(null);

        handler.on(event());

        ArgumentCaptor<CreateAlertCommand> captor = ArgumentCaptor.forClass(CreateAlertCommand.class);
        verify(notificationService).createAlert(captor.capture());
        CreateAlertCommand command = captor.getValue();
        assertThat(command.userId()).isEqualTo(9L);
        assertThat(command.sourceType()).isEqualTo("DEVICE");
        assertThat(command.sourceId()).isEqualTo("33");
        assertThat(command.sourceLabel()).isEqualTo("Aire acondicionado");
        assertThat(command.title()).isEqualTo("Consumo elevado en Aire acondicionado");
        assertThat(command.message()).contains("2500").contains("1800");
        assertThat(command.threadKey()).isEqualTo("ENERGY:DEVICE:33:THRESHOLD");
    }

    @Test
    void evaluatesTheUserRulesForTheDeviceInvolved() {
        handler.on(event());

        verify(notificationService).createAlertFromRuleEvaluation(
                eq(9L),
                eq(RuleScopeType.DEVICE),
                eq("33"),
                eq(new BigDecimal("2500")),
                eq("Aire acondicionado")
        );
    }

    private EnergyThresholdExceededIntegrationEvent event() {
        return new EnergyThresholdExceededIntegrationEvent(
                9L,
                33L,
                "Aire acondicionado",
                new BigDecimal("2500"),
                new BigDecimal("1800"),
                LocalDateTime.of(2026, 11, 5, 10, 30)
        );
    }
}
