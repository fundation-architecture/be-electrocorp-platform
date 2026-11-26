package com.electrocorp.electrocorpplatform.shared.infrastructure.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Reloj de la plataforma. Las rutinas programadas (US-11) se ejecutan en la hora local del usuario
 * (America/Lima por defecto), aunque el servidor o el contenedor Docker trabajen en UTC.
 * La zona tambien se fija como zona por defecto de la JVM para que las fechas de auditoria
 * (createdAt) y la hora de ejecucion de las rutinas usen la misma referencia.
 */
@Configuration
public class ClockConfiguration {

    @Bean
    public Clock platformClock(@Value("${electrocorp.time-zone:America/Lima}") String timeZone) {
        ZoneId zoneId = ZoneId.of(timeZone);
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
        return Clock.system(zoneId);
    }
}
