package com.electrocorp.electrocorpplatform.iam.domain.model.commands;

public record UpdateUiPreferenceCommand(
        String language,
        String theme
) {
}
