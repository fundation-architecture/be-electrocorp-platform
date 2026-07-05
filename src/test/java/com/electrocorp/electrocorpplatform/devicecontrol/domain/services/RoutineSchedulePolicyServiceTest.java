package com.electrocorp.electrocorpplatform.devicecontrol.domain.services;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineAction;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineRepeatType;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineTargetType;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Routine;
import com.electrocorp.electrocorpplatform.shared.domain.model.AuditableEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutineSchedulePolicyServiceTest {

    private final RoutineSchedulePolicyService policyService = new RoutineSchedulePolicyService();

    @Test
    void dailyRoutineIsDueOncePerScheduledOccurrence() {
        Routine routine = routine(RoutineRepeatType.DAILY, "08:00");
        LocalDateTime firstOccurrence = LocalDateTime.of(2026, 7, 6, 8, 5);

        assertTrue(policyService.isDue(routine, firstOccurrence));

        routine.markExecutedAt(firstOccurrence);

        assertFalse(policyService.isDue(routine, LocalDateTime.of(2026, 7, 6, 9, 0)));
        assertTrue(policyService.isDue(routine, LocalDateTime.of(2026, 7, 7, 8, 0)));
    }

    @Test
    void weeklyRoutineUsesSelectedDays() {
        Routine routine = routine(RoutineRepeatType.WEEKLY, "18:30");
        routine.setDaysOfWeek("MON,WED");

        assertTrue(policyService.isDue(routine, LocalDateTime.of(2026, 7, 6, 18, 30)));
        assertFalse(policyService.isDue(routine, LocalDateTime.of(2026, 7, 7, 18, 30)));
    }

    @Test
    void customIntervalRoutineUsesCreationDateAsAnchor() throws Exception {
        Routine routine = routine(RoutineRepeatType.CUSTOM_INTERVAL, "07:00");
        routine.setIntervalDays(2);
        setCreatedAt(routine, LocalDateTime.of(2026, 7, 1, 6, 0));

        assertTrue(policyService.isDue(routine, LocalDateTime.of(2026, 7, 3, 7, 0)));
        assertFalse(policyService.isDue(routine, LocalDateTime.of(2026, 7, 4, 7, 0)));
    }

    @Test
    void onceRoutineIsDueOnStartDateAndThenStops() {
        Routine routine = routine(RoutineRepeatType.ONCE, "10:30");
        routine.setStartsOn("2026-07-06");

        assertFalse(policyService.isDue(routine, LocalDateTime.of(2026, 7, 6, 10, 29)));
        assertTrue(policyService.isDue(routine, LocalDateTime.of(2026, 7, 6, 10, 30)));

        routine.markExecutedAt(LocalDateTime.of(2026, 7, 6, 10, 30));

        assertFalse(policyService.isDue(routine, LocalDateTime.of(2026, 7, 6, 11, 0)));
    }

    @Test
    void weeklyScheduleRequiresAtLeastOneDay() {
        assertThrows(IllegalArgumentException.class, () -> policyService.validateSchedule(
                "08:00",
                RoutineRepeatType.WEEKLY,
                "",
                1,
                null
        ));
    }

    private Routine routine(RoutineRepeatType repeatType, String time) {
        Routine routine = new Routine();
        routine.setUserId(1L);
        routine.setTargetType(RoutineTargetType.DEVICE);
        routine.setTargetId(10L);
        routine.setDeviceId(10L);
        routine.setName("Morning routine");
        routine.setAction(RoutineAction.TURN_ON);
        routine.setTime(time);
        routine.setRepeatType(repeatType);
        routine.setEnabled(true);
        return routine;
    }

    private void setCreatedAt(Routine routine, LocalDateTime createdAt) throws Exception {
        Field createdAtField = AuditableEntity.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(routine, createdAt);
    }
}
