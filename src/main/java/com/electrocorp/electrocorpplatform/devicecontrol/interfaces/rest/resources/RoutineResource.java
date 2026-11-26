package com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources;

import java.time.ZoneId;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Routine;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineAction;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineRepeatType;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineTargetType;

public record RoutineResource(
        Long id,
        Long userId,
        Long deviceId,
        Long groupId,
        RoutineTargetType targetType,
        Long targetId,
        String targetName,
        String name,
        RoutineAction action,
        String time,
        RoutineRepeatType repeatType,
        String daysOfWeek,
        Integer intervalDays,
        String startsOn,
        Integer applicableDeviceCount,
        Integer blockedDeviceCount,
        Boolean enabled,
        String timeZone,
        String lastExecutedAt
) {
    public static RoutineResource from(
            Routine routine,
            String targetName,
            int applicableDeviceCount,
            int blockedDeviceCount
    ) {
        return new RoutineResource(
                routine.getId(),
                routine.getUserId(),
                routine.getDeviceId(),
                routine.getGroupId(),
                routine.getTargetType(),
                routine.getTargetId(),
                targetName,
                routine.getName(),
                routine.getAction(),
                routine.getTime(),
                routine.getEffectiveRepeatType(),
                routine.getDaysOfWeek(),
                routine.getEffectiveIntervalDays(),
                routine.getStartsOn(),
                applicableDeviceCount,
                blockedDeviceCount,
                routine.getEnabled(),
                ZoneId.systemDefault().getId(),
                routine.getLastExecutedAt() == null ? null : routine.getLastExecutedAt().toString()
        );
    }

    public static RoutineResource from(Routine routine) {
        return from(routine, "", 0, 0);
    }
}
