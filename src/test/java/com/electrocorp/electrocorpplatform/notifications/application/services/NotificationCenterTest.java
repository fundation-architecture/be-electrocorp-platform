package com.electrocorp.electrocorpplatform.notifications.application.services;

import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.Alert;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.AlertRepository;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.AlertRuleProfileRepository;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.AlertRuleRepository;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.NotificationPreferenceRepository;
import com.electrocorp.electrocorpplatform.notifications.domain.services.AlertPolicyService;
import com.electrocorp.electrocorpplatform.notifications.domain.services.AlertRulePolicyService;
import com.electrocorp.electrocorpplatform.notifications.domain.services.AlertSourceOwnershipService;
import com.electrocorp.electrocorpplatform.notifications.domain.services.NotificationDeliveryPolicyService;
import com.electrocorp.electrocorpplatform.notifications.domain.services.NotificationPreferencePolicyService;
import com.electrocorp.electrocorpplatform.notifications.domain.services.RuleEvaluationService;
import com.electrocorp.electrocorpplatform.notifications.infrastructure.delivery.NotificationDeliveryPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T-17 (US-21): el centro de notificaciones devuelve solo las alertas del usuario, de la mas
 * reciente a la mas antigua, y una lista vacia cuando no hay alertas.
 */
class NotificationCenterTest {

    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final NotificationApplicationService service = new NotificationApplicationService(
            alertRepository,
            mock(AlertRuleRepository.class),
            mock(AlertRuleProfileRepository.class),
            mock(NotificationPreferenceRepository.class),
            mock(AlertPolicyService.class),
            mock(AlertSourceOwnershipService.class),
            mock(RuleEvaluationService.class),
            mock(NotificationPreferencePolicyService.class),
            mock(NotificationDeliveryPolicyService.class),
            mock(NotificationDeliveryPort.class),
            new AlertRulePolicyService()
    );

    @Test
    void returnsOnlyTheUserAlertsFromNewestToOldest() {
        Alert older = alert(7L, "Consumo elevado en Terma", LocalDateTime.of(2026, 11, 20, 9, 0));
        Alert newer = alert(7L, "Consumo elevado en Horno", LocalDateTime.of(2026, 11, 25, 18, 0));
        Alert foreign = alert(8L, "Alerta de otro usuario", LocalDateTime.of(2026, 11, 26, 8, 0));
        when(alertRepository.findByUserId(7L)).thenReturn(new ArrayList<>(List.of(older, foreign, newer)));

        List<Alert> alerts = service.getAlerts(7L);

        verify(alertRepository).findByUserId(7L);
        assertThat(alerts).extracting(Alert::getTitle)
                .containsExactly("Consumo elevado en Horno", "Consumo elevado en Terma");
    }

    @Test
    void returnsAnEmptyListWhenTheUserHasNoAlerts() {
        when(alertRepository.findByUserId(9L)).thenReturn(List.of());

        assertThat(service.getAlerts(9L)).isEmpty();
    }

    @Test
    void returnsAnEmptyListWithoutAnAuthenticatedUser() {
        assertThat(service.getAlerts(null)).isEmpty();
        verifyNoInteractions(alertRepository);
    }

    private Alert alert(Long userId, String title, LocalDateTime lastTriggeredAt) {
        Alert alert = new Alert();
        alert.setUserId(userId);
        alert.setTitle(title);
        alert.setActive(true);
        alert.setFirstDetectedAt(lastTriggeredAt);
        alert.setLastTriggeredAt(lastTriggeredAt);
        return alert;
    }
}
