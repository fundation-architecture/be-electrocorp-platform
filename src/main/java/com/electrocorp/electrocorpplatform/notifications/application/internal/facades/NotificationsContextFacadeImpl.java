package com.electrocorp.electrocorpplatform.notifications.application.internal.facades;

import com.electrocorp.electrocorpplatform.notifications.application.facades.ActiveAlertsOverview;
import com.electrocorp.electrocorpplatform.notifications.application.facades.NotificationsContextFacade;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.Alert;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.AlertRule;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.RuleScopeType;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.AlertRepository;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationsContextFacadeImpl implements NotificationsContextFacade {

    private static final Set<String> LIMIT_CONDITIONS = Set.of("GREATER_THAN", "GREATER_OR_EQUAL_THAN");

    private final AlertRepository alertRepository;
    private final AlertRuleRepository alertRuleRepository;

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

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> findConsumptionLimitWatts(Long userId, Long deviceId) {
        if (userId == null || deviceId == null) {
            return Optional.empty();
        }

        return alertRuleRepository.findByUserIdAndEnabledTrue(userId).stream()
                .filter(rule -> "WATTS".equalsIgnoreCase(rule.getMetric()))
                .filter(rule -> LIMIT_CONDITIONS.contains(String.valueOf(rule.getConditionType()).toUpperCase()))
                .filter(rule -> rule.getThreshold() != null && rule.getThreshold().signum() > 0)
                .filter(rule -> rule.appliesTo(RuleScopeType.DEVICE, String.valueOf(deviceId)))
                .map(AlertRule::getThreshold)
                .min(Comparator.naturalOrder());
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
