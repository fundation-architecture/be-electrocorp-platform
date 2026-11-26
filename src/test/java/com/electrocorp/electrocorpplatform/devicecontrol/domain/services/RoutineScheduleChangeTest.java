package com.electrocorp.electrocorpplatform.devicecontrol.domain.services;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineAction;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineRepeatType;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineTargetType;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Routine;
import com.electrocorp.electrocorpplatform.shared.domain.model.AuditableEntity;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-09 (US-12): al modificar el horario, las ocurrencias anteriores al cambio no se ejecutan y
 * la nueva hora se respeta.
 */
class RoutineScheduleChangeTest {

    private final RoutineSchedulePolicyService policy = new RoutineSchedulePolicyService();

    @Test
    void movingTheScheduleLaterRunsAtTheNewTimeAndNotAtTheOldOne() {
        Routine routine = dailyRoutine("18:00", LocalDateTime.of(2026, 11, 20, 9, 0));
        routine.setTime("19:00");
        routine.setScheduleChangedAt(LocalDateTime.of(2026, 11, 26, 17, 30));

        assertThat(policy.isDue(routine, LocalDateTime.of(2026, 11, 26, 18, 5))).isFalse();
        assertThat(policy.isDue(routine, LocalDateTime.of(2026, 11, 26, 19, 0))).isTrue();
    }

    @Test
    void movingTheScheduleToAnHourThatAlreadyPassedWaitsUntilTheNextDay() {
        Routine routine = dailyRoutine("20:00", LocalDateTime.of(2026, 11, 20, 9, 0));
        routine.setTime("08:00");
        routine.setScheduleChangedAt(LocalDateTime.of(2026, 11, 26, 10, 0));

        assertThat(policy.isDue(routine, LocalDateTime.of(2026, 11, 26, 10, 1))).isFalse();
        assertThat(policy.isDue(routine, LocalDateTime.of(2026, 11, 27, 8, 0))).isTrue();
    }

    @Test
    void withoutChangesTheCreationDateIsTheReference() {
        Routine routine = dailyRoutine("08:00", LocalDateTime.of(2026, 11, 26, 9, 0));

        assertThat(routine.getScheduleEffectiveFrom()).isEqualTo(LocalDateTime.of(2026, 11, 26, 9, 0));
        assertThat(policy.isDue(routine, LocalDateTime.of(2026, 11, 26, 9, 30))).isFalse();
    }

    private Routine dailyRoutine(String time, LocalDateTime createdAt) {
        Routine routine = new Routine();
        ReflectionTestUtils.setField(routine, AuditableEntity.class, "createdAt", createdAt, LocalDateTime.class);
        routine.setUserId(7L);
        routine.setTargetType(RoutineTargetType.DEVICE);
        routine.setTargetId(10L);
        routine.setDeviceId(10L);
        routine.setName("Apagar terma");
        routine.setAction(RoutineAction.TURN_OFF);
        routine.setTime(time);
        routine.setRepeatType(RoutineRepeatType.DAILY);
        routine.setEnabled(true);
        return routine;
    }
}
