package com.electrocorp.electrocorpplatform.iam.domain.repositories;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.AccessProfile;

import java.util.List;
import java.util.Optional;

public interface AccessProfileRepository {
    List<AccessProfile> findAll();

    Optional<AccessProfile> findByName(String name);

    Optional<AccessProfile> findById(Long id);

    AccessProfile save(AccessProfile accessProfile);
}
