package com.electrocorp.electrocorpplatform.devicecontrol.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.RoutineRepository;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.services.RoutineSchedulePolicyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Ejecuta las rutinas de encendido y apagado programadas (US-11). La hora se toma del reloj de la
 * plataforma (electrocorp.time-zone, America/Lima por defecto) para que una rutina de las 19:00 se
 * ejecute a las 19:00 hora del usuario aunque el servidor trabaje en UTC.
 */
@Service
@RequiredArgsConstructor
public class RoutineSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutineSchedulerService.class);

    private final RoutineRepository routineRepository;
    private final RoutineSchedulePolicyService routineSchedulePolicyService;
    private final DeviceControlApplicationService deviceControlApplicationService;
    private final Clock platformClock;

    @Scheduled(
            fixedDelayString = "${electrocorp.routines.scheduler-delay-ms:60000}",
            initialDelayString = "${electrocorp.routines.scheduler-initial-delay-ms:30000}"
    )
    public void executeDueRoutines() {
        LocalDateTime now = LocalDateTime.now(platformClock);

        routineRepository.findByEnabledTrue()
                .stream()
                .filter(routine -> routineSchedulePolicyService.isDue(routine, now))
                .forEach(routine -> executeRoutine(routine.getUserId(), routine.getId(), now));
    }

    private void executeRoutine(Long userId, Long routineId, LocalDateTime executedAt) {
        try {
            deviceControlApplicationService.executeScheduledRoutine(userId, routineId, executedAt);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not execute scheduled routine {}", routineId, exception);
        }
    }
}
