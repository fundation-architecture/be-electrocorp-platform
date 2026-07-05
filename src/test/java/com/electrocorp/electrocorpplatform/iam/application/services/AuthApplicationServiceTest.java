package com.electrocorp.electrocorpplatform.iam.application.services;

import com.electrocorp.electrocorpplatform.iam.application.ports.PasswordRecoveryDeliveryPort;
import com.electrocorp.electrocorpplatform.iam.application.security.HashingService;
import com.electrocorp.electrocorpplatform.iam.application.security.PasswordResetTokenService;
import com.electrocorp.electrocorpplatform.iam.application.security.TokenService;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.RecoverPasswordCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.ResetPasswordCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.entities.PasswordResetToken;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.AccessProfileRepository;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.PasswordResetTokenRepository;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.UserRepository;
import com.electrocorp.electrocorpplatform.iam.domain.services.PasswordPolicyService;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthApplicationServiceTest {

    @Test
    void recoverPasswordStoresHashedOneTimeTokenAndDeliversRawToken() {
        TestContext context = new TestContext();
        User user = mock(User.class);
        PasswordResetToken previousToken = new PasswordResetToken(
                7L,
                "previous-token-hash",
                LocalDateTime.now().plusMinutes(10)
        );

        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn("ada@gmail.com");
        when(user.isActive()).thenReturn(true);
        when(context.userRepository.findByEmail("ada@gmail.com")).thenReturn(Optional.of(user));
        when(context.passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(7L)).thenReturn(List.of(previousToken));
        when(context.passwordResetTokenService.generateToken()).thenReturn("raw-reset-token");
        when(context.passwordResetTokenService.hashToken("raw-reset-token")).thenReturn("hashed-reset-token");

        context.service.recoverPassword(new RecoverPasswordCommand(" Ada@Gmail.com "));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(context.passwordResetTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
        verify(context.passwordResetTokenRepository, times(2)).save(tokenCaptor.capture());
        verify(context.passwordRecoveryDeliveryPort).sendPasswordResetLink(user, "raw-reset-token");

        PasswordResetToken savedToken = tokenCaptor.getAllValues().get(1);
        assertEquals(7L, savedToken.getUserId());
        assertEquals("hashed-reset-token", savedToken.getTokenHash());
        assertNotNull(previousToken.getUsedAt());
    }

    @Test
    void recoverPasswordDoesNotRevealUnknownEmail() {
        TestContext context = new TestContext();

        when(context.userRepository.findByEmail("missing@gmail.com")).thenReturn(Optional.empty());

        context.service.recoverPassword(new RecoverPasswordCommand("missing@gmail.com"));

        verify(context.passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(context.passwordRecoveryDeliveryPort, never()).sendPasswordResetLink(any(User.class), any(String.class));
    }

    @Test
    void resetPasswordConsumesTokenAndStoresNewPasswordHash() {
        TestContext context = new TestContext();
        User user = mock(User.class);
        PasswordResetToken resetToken = new PasswordResetToken(
                9L,
                "hashed-reset-token",
                LocalDateTime.now().plusMinutes(10)
        );

        when(user.isActive()).thenReturn(true);
        when(context.passwordResetTokenService.hashToken("raw-reset-token")).thenReturn("hashed-reset-token");
        when(context.passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull("hashed-reset-token"))
                .thenReturn(Optional.of(resetToken));
        when(context.userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(context.hashingService.hash("NewPass123")).thenReturn("$2hashed-password");

        context.service.resetPassword(new ResetPasswordCommand("raw-reset-token", "NewPass123"));

        verify(user).setPasswordHash("$2hashed-password");
        verify(context.userRepository).save(user);
        verify(context.passwordResetTokenRepository).save(resetToken);
        assertNotNull(resetToken.getUsedAt());
    }

    private static class TestContext {
        private final UserRepository userRepository = mock(UserRepository.class);
        private final AccessProfileRepository accessProfileRepository = mock(AccessProfileRepository.class);
        private final HashingService hashingService = mock(HashingService.class);
        private final TokenService tokenService = mock(TokenService.class);
        private final DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
        private final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
        private final PasswordResetTokenService passwordResetTokenService = mock(PasswordResetTokenService.class);
        private final PasswordRecoveryDeliveryPort passwordRecoveryDeliveryPort = mock(PasswordRecoveryDeliveryPort.class);
        private final AuthApplicationService service = new AuthApplicationService(
                userRepository,
                accessProfileRepository,
                hashingService,
                tokenService,
                domainEventPublisher,
                passwordResetTokenRepository,
                passwordResetTokenService,
                passwordRecoveryDeliveryPort,
                new PasswordPolicyService()
        );
    }
}
