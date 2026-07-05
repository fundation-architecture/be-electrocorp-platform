package com.electrocorp.electrocorpplatform.iam.application.security;

public interface PasswordResetTokenService {
    String generateToken();

    String hashToken(String token);
}
