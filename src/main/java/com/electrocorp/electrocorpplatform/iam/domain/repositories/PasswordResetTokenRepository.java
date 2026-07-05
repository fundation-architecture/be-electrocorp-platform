package com.electrocorp.electrocorpplatform.iam.domain.repositories;

import com.electrocorp.electrocorpplatform.iam.domain.model.entities.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository {
    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId);

    PasswordResetToken save(PasswordResetToken passwordResetToken);

    void deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
