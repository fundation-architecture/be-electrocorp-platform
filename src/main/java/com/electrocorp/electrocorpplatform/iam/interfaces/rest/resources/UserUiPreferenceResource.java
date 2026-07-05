package com.electrocorp.electrocorpplatform.iam.interfaces.rest.resources;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.UserUiPreference;

public record UserUiPreferenceResource(
        Long id,
        Long userId,
        String language,
        String theme
) {
    public static UserUiPreferenceResource from(UserUiPreference preference) {
        return new UserUiPreferenceResource(
                preference.getId(),
                preference.getUserId(),
                preference.getLanguage(),
                preference.getTheme()
        );
    }
}
