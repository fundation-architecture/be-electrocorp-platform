package com.electrocorp.electrocorpplatform.devicecontrol.domain.services;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineAction;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineExecutionStatus;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineRepeatType;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineTargetType;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Routine;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-06 (US-11): no se registran dos programaciones identicas y cada ejecucion deja su resultado.
 */
class RoutineConflictAndExecutionTest {

    private final RoutineConflictCheckerService conflictChecker = new RoutineConflictCheckerService();

    @Test
    void detectsAnIdenticalRoutineForTheSameTarget() {
        Routine existing = routine(10L, "18:00", RoutineAction.TURN_OFF);

        assertThat(conflictChecker.hasConflict(List.of(existing), routine(10L, "18:00", RoutineAction.TURN_OFF))).isTrue();
        assertThat(conflictChecker.hasConflict(List.of(existing), routine(10L, "19:00", RoutineAction.TURN_OFF))).isFalse();
        assertThat(conflictChecker.hasConflict(List.of(existing), routine(10L, "18:00", RoutineAction.TURN_ON))).isFalse();
        assertThat(conflictChecker.hasConflict(List.of(existing), routine(11L, "18:00", RoutineAction.TURN_OFF))).isFalse();
    }

    @Test
    void recordingAnExecutionKeepsTheResultAndMarksTheOccurrence() {
        Routine routine = routine(10L, "18:00", RoutineAction.TURN_OFF);
        LocalDateTime executedAt = LocalDateTime.of(2026, 11, 26, 18, 0);

        routine.recordExecution(RoutineExecutionStatus.NOT_APPLIED, "Dispositivo en mantenimiento", executedAt);

        assertThat(routine.getLastExecutionStatus()).isEqualTo(RoutineExecutionStatus.NOT_APPLIED);
        assertThat(routine.getLastExecutionMessage()).isEqualTo("Dispositivo en mantenimiento");
        assertThat(routine.getLastExecutedAt()).isEqualTo(executedAt);
        assertThat(routine.isEnabled()).isTrue();
    }

    private Routine routine(Long targetId, String time, RoutineAction action) {
        Routine routine = new Routine();
        routine.setUserId(7L);
        routine.setTargetType(RoutineTargetType.DEVICE);
        routine.setTargetId(targetId);
        routine.setDeviceId(targetId);
        routine.setName("Apagar terma");
        routine.setAction(action);
        routine.setTime(time);
        routine.setRepeatType(RoutineRepeatType.DAILY);
        routine.setEnabled(true);
        return routine;
    }
}
