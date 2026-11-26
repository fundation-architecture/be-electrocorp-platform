package com.electrocorp.electrocorpplatform.notifications.application.facades;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Fachada de Notifications para otros bounded contexts. Evita que, por ejemplo, Energy Monitoring
 * dependa de Alert, AlertRule o de sus repositorios.
 */
public interface NotificationsContextFacade {

    ActiveAlertsOverview getActiveAlertsOverview(Long userId);

    /**
     * Limite de consumo en watts que el usuario configuro para el dispositivo (US-19): la menor
     * regla activa de metrica WATTS y condicion "mayor que" que aplica al dispositivo.
     */
    default Optional<BigDecimal> findConsumptionLimitWatts(Long userId, Long deviceId) {
        return Optional.empty();
    }
}
