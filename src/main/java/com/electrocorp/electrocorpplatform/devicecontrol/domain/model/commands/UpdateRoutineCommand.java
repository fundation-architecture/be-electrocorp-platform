package com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineAction;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineRepeatType;

/**
 * Modificacion de una programacion existente (US-12). Los campos nulos conservan su valor actual.
 */
public record UpdateRoutineCommand(
        Long userId,
        Long routineId,
        String name,
        RoutineAction action,
        String time,
        RoutineRepeatType repeatType,
        String daysOfWeek,
        Integer intervalDays,
        String startsOn
) {
}
