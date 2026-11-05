package com.electrocorp.electrocorpplatform.notifications.application.internal.facades;

import com.electrocorp.electrocorpplatform.notifications.application.facades.ActiveAlertsOverview;
import com.electrocorp.electrocorpplatform.notifications.application.facades.NotificationsContextFacade;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.Alert;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationsContextFacadeImpl implements NotificationsContextFacade {

    private final AlertRepository alertRepository;

    @Override
    @Transactional(readOnly = true)
    public ActiveAlertsOverview getActiveAlertsOverview(Long userId) {
        List<Alert> activeAlerts = alertRepository.findByUserIdAndActiveTrue(userId);
        if (activeAlerts.isEmpty()) {
            return ActiveAlertsOverview.empty();
        }

        int criticalAlerts = (int) activeAlerts.stream()
                .filter(Alert::isCritical)
                .count();
        Alert latestAlert = activeAlerts.stream()
                .max(Comparator.comparing(this::lastTriggeredAt))
                .orElseThrow();

        return new ActiveAlertsOverview(
                activeAlerts.size(),
                criticalAlerts,
                latestAlert.getLevel(),
                latestAlert.getTitle()
        );
    }

    private LocalDateTime lastTriggeredAt(Alert alert) {
        if (alert.getLastTriggeredAt() != null) {
            return alert.getLastTriggeredAt();
        }

        if (alert.getFirstDetectedAt() != null) {
            return alert.getFirstDetectedAt();
        }

        return LocalDateTime.MIN;
    }
}
