package com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Routine;

import java.util.List;
import java.util.Optional;

public interface RoutineRepository {
    List<Routine> findByUserId(Long userId);

    List<Routine> findByEnabledTrue();

    Optional<Routine> findByIdAndUserId(Long id, Long userId);

    Routine save(Routine routine);

    void delete(Routine routine);
}
