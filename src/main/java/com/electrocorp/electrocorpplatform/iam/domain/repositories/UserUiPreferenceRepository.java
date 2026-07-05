package com.electrocorp.electrocorpplatform.iam.domain.repositories;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.UserUiPreference;

import java.util.Optional;

public interface UserUiPreferenceRepository {
    Optional<UserUiPreference> findByUserId(Long userId);

    UserUiPreference save(UserUiPreference preference);
}
