package com.electrocorp.electrocorpplatform.iam.domain.repositories;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<User> findAll();

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findById(Long id);

    User save(User user);
}
