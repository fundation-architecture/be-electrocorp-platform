package com.electrocorp.electrocorpplatform.iam.application.security;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;

import java.util.Optional;

public interface TokenService {
    String generateToken(User user);

    Optional<Long> validateAndGetUserId(String token);
}
