package com.electrocorp.electrocorpplatform.iam.infrastructure.persistence.jpa.repositories;

import com.electrocorp.electrocorpplatform.iam.domain.model.entities.PasswordResetToken;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.PasswordResetTokenRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long>, PasswordResetTokenRepository {
}
