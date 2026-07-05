package com.electrocorp.electrocorpplatform.iam.domain.model.aggregates;

import com.electrocorp.electrocorpplatform.shared.domain.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "user_ui_preferences")
public class UserUiPreference extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String language = "en";

    @Column(nullable = false, length = 10)
    private String theme = "dark";

    public UserUiPreference(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id must be positive.");
        }

        this.userId = userId;
    }

    public void update(String language, String theme) {
        if (language != null && !language.isBlank()) {
            this.language = normalizeLanguage(language);
        }

        if (theme != null && !theme.isBlank()) {
            this.theme = normalizeTheme(theme);
        }
    }

    private String normalizeLanguage(String value) {
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "es", "en", "pt" -> normalized;
            default -> throw new IllegalArgumentException("UI language is invalid.");
        };
    }

    private String normalizeTheme(String value) {
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "light", "dark" -> normalized;
            default -> throw new IllegalArgumentException("UI theme is invalid.");
        };
    }
}
