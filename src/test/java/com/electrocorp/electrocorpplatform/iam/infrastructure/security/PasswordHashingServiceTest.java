package com.electrocorp.electrocorpplatform.iam.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashingServiceTest {

    @Test
    void hashStoresPasswordAsBcryptAndMatchesRawValue() {
        PasswordHashingService hashingService = new PasswordHashingService(new BCryptPasswordEncoder());

        String hash = hashingService.hash("Secure123!");

        assertNotEquals("Secure123!", hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(hashingService.matches("Secure123!", hash));
        assertFalse(hashingService.matches("wrong-password", hash));
    }
}
