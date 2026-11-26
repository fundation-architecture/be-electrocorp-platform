package com.electrocorp.electrocorpplatform.devicecontrol.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineAction;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineRepeatType;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineTargetType;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Routine;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.RoutineRepository;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.services.RoutineSchedulePolicyService;
import com.electrocorp.electrocorpplatform.shared.domain.model.AuditableEntity;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-02 (US-11): el planificador ejecuta la rutina a la hora local de la plataforma (America/Lima),
 * aunque el servidor trabaje en UTC.
 */
class RoutineSchedulerServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final RoutineRepository routineRepository = mock(RoutineRepository.class);
    private final DeviceControlApplicationService deviceControlApplicationService = mock(DeviceControlApplicationService.class);

    @Test
    void executesTheRoutineAtItsLocalTimeInLima() {
        Routine routine = dailyRoutineAt("19:00");
        when(routineRepository.findByEnabledTrue()).thenReturn(List.of(routine));
        // 00:05 UTC del 27/11 equivale a 19:05 del 26/11 en Lima.
        RoutineSchedulerService scheduler = schedulerAt("2026-11-27T00:05:00Z");

        scheduler.executeDueRoutines();

        verify(deviceControlApplicationService).executeScheduledRoutine(
                7L, 40L, LocalDateTime.of(2026, 11, 26, 19, 5));
    }

    @Test
    void doesNotExecuteTheRoutineAtTheSameHourInUtc() {
        Routine routine = dailyRoutineAt("19:00");
        when(routineRepository.findByEnabledTrue()).thenReturn(List.of(routine));
        // 19:05 UTC equivale a 14:05 en Lima: la rutina de las 19:00 todavia no debe ejecutarse.
        RoutineSchedulerService scheduler = schedulerAt("2026-11-26T19:05:00Z");

        scheduler.executeDueRoutines();

        verify(deviceControlApplicationService, never()).executeScheduledRoutine(anyLong(), anyLong(), any());
    }

    @Test
    void doesNotExecuteDisabledRoutines() {
        Routine routine = dailyRoutineAt("19:00");
        routine.setEnabled(false);
        when(routineRepository.findByEnabledTrue()).thenReturn(List.of(routine));

        schedulerAt("2026-11-27T00:05:00Z").executeDueRoutines();

        verify(deviceControlApplicationService, never()).executeScheduledRoutine(anyLong(), anyLong(), any());
    }

    private RoutineSchedulerService schedulerAt(String instant) {
        return new RoutineSchedulerService(
                routineRepository,
                new RoutineSchedulePolicyService(),
                deviceControlApplicationService,
                Clock.fixed(Instant.parse(instant), LIMA)
        );
    }

    private Routine dailyRoutineAt(String time) {
        Routine routine = new Routine();
        ReflectionTestUtils.setField(routine, "id", 40L);
        ReflectionTestUtils.setField(routine, AuditableEntity.class, "createdAt", LocalDateTime.of(2026, 11, 26, 8, 0), LocalDateTime.class);
        routine.setUserId(7L);
        routine.setTargetType(RoutineTargetType.DEVICE);
        routine.setTargetId(10L);
        routine.setDeviceId(10L);
        routine.setName("Encender terma");
        routine.setAction(RoutineAction.TURN_ON);
        routine.setTime(time);
        routine.setRepeatType(RoutineRepeatType.DAILY);
        routine.setEnabled(true);
        return routine;
    }
}
