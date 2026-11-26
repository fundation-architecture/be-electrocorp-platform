package com.electrocorp.electrocorpplatform.devicecontrol.domain.services;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Routine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class RoutineConflictCheckerService {

    public boolean hasConflict(List<Routine> routines, Routine candidate) {
        if (routines == null || candidate == null) {
            return false;
        }

        return routines.stream()
                .anyMatch(existing ->
                        existing.getTargetType() == candidate.getTargetType()
                                && Objects.equals(existing.getTargetId(), candidate.getTargetId())
                                && Objects.equals(existing.getTime(), candidate.getTime())
                                && existing.getAction() == candidate.getAction()
                );
    }
}