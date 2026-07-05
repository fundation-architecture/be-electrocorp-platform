package com.electrocorp.electrocorpplatform.marketing.domain.model.aggregates;

import com.electrocorp.electrocorpplatform.shared.domain.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "newsletter_subscriptions")
public class NewsletterSubscription extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(length = 80)
    private String source;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private LocalDateTime subscribedAt;

    public NewsletterSubscription(String email, String source) {
        this.email = normalizeEmail(email);
        this.source = normalizeSource(source);
        this.active = true;
        this.subscribedAt = LocalDateTime.now();
    }

    public void reactivate(String source) {
        this.active = true;
        this.source = normalizeSource(source);
        this.subscribedAt = LocalDateTime.now();
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank() || !value.contains("@")) {
            throw new IllegalArgumentException("Newsletter email is invalid.");
        }

        return value.trim().toLowerCase();
    }

    private String normalizeSource(String value) {
        if (value == null || value.isBlank()) {
            return "landing";
        }

        String normalized = value.trim();
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80);
    }
}
