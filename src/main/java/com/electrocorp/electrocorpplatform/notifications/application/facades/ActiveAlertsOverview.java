package com.electrocorp.electrocorpplatform.notifications.application.facades;

/**
 * Resumen de las alertas activas de un usuario que Notifications expone a otros bounded contexts.
 */
public record ActiveAlertsOverview(
        int activeAlerts,
        int criticalAlerts,
        String latestAlertLevel,
        String latestAlertTitle
) {
    public static ActiveAlertsOverview empty() {
        return new ActiveAlertsOverview(0, 0, null, null);
    }
}
