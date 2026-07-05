package com.electrocorp.electrocorpplatform.iam.domain.model.entities;

import com.electrocorp.electrocorpplatform.shared.domain.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "password_reset_tokens",
        indexes = {
                @Index(name = "idx_password_reset_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_password_reset_tokens_hash", columnList = "token_hash")
        }
)
public class PasswordResetToken extends AuditableEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public PasswordResetToken(Long userId, String tokenHash, LocalDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsableAt(LocalDateTime checkedAt) {
        return usedAt == null && expiresAt.isAfter(checkedAt);
    }

    public void markUsed(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
}
