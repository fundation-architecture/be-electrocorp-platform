package com.electrocorp.electrocorpplatform.notifications.application.facades;

/**
 * Fachada de Notifications para otros bounded contexts. Evita que, por ejemplo, Energy Monitoring
 * dependa de Alert o de AlertRepository.
 */
public interface NotificationsContextFacade {

    ActiveAlertsOverview getActiveAlertsOverview(Long userId);
}
