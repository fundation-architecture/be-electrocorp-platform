package com.electrocorp.electrocorpplatform.iam.domain.model.commands;

public record ResetPasswordCommand(
        String token,
        String password
) {
}
