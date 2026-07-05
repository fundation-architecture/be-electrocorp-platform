package com.electrocorp.electrocorpplatform.iam.infrastructure.persistence.jpa.repositories;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.UserUiPreference;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.UserUiPreferenceRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserUiPreferenceRepository extends JpaRepository<UserUiPreference, Long>, UserUiPreferenceRepository {
}
