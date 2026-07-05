package com.electrocorp.electrocorpplatform.iam.application.services;

import com.electrocorp.electrocorpplatform.iam.application.commandservices.AuthCommandService;
import com.electrocorp.electrocorpplatform.iam.application.ports.PasswordRecoveryDeliveryPort;
import com.electrocorp.electrocorpplatform.iam.application.queryservices.AuthenticatedUserQueryService;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.RecoverPasswordCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.ResetPasswordCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.SignInCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.SignUpCommand;
import com.electrocorp.electrocorpplatform.iam.application.results.AuthenticationResult;
import com.electrocorp.electrocorpplatform.iam.application.security.HashingService;
import com.electrocorp.electrocorpplatform.iam.application.security.PasswordResetTokenService;
import com.electrocorp.electrocorpplatform.iam.application.security.TokenService;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.AccessProfile;
import com.electrocorp.electrocorpplatform.iam.domain.model.AccountStatus;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;
import com.electrocorp.electrocorpplatform.iam.domain.model.entities.PasswordResetToken;
import com.electrocorp.electrocorpplatform.iam.domain.model.events.UserRegisteredEvent;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.AccessProfileRepository;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.PasswordResetTokenRepository;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.UserRepository;
import com.electrocorp.electrocorpplatform.iam.domain.services.PasswordPolicyService;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthApplicationService implements AuthCommandService, AuthenticatedUserQueryService {

    private static final long PASSWORD_RESET_EXPIRATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final AccessProfileRepository accessProfileRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final DomainEventPublisher domainEventPublisher;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetTokenService passwordResetTokenService;
    private final PasswordRecoveryDeliveryPort passwordRecoveryDeliveryPort;
    private final PasswordPolicyService passwordPolicyService;

    @Override
    @Transactional
    public AuthenticationResult signUp(SignUpCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Email already exists.");
        }

        AccessProfile profile = accessProfileRepository.findByName("OWNER")
                .orElseGet(() -> accessProfileRepository.save(new AccessProfile("OWNER", "Account owner profile")));

        User user = new User();
        user.setFullName(command.fullName());
        user.setEmail(command.email());
        user.setPasswordHash(hashingService.hash(command.password()));
        user.setStatus(AccountStatus.ACTIVE);
        user.setAccessProfile(profile);

        User savedUser = userRepository.save(user);
        domainEventPublisher.publish(new UserRegisteredEvent(savedUser.getId(), savedUser.getEmail()));

        return new AuthenticationResult(savedUser, tokenService.generateToken(savedUser));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResult signIn(SignInCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials."));

        if (!hashingService.matches(command.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials.");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("Account is not active.");
        }

        return new AuthenticationResult(user, tokenService.generateToken(user));
    }

    @Override
    @Transactional(readOnly = true)
    public User getAuthenticatedUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));
    }

    @Override
    @Transactional
    public void signOut() {
    }

    @Override
    @Transactional
    public void recoverPassword(RecoverPasswordCommand command) {
        String email = normalizeEmail(command.email());
        LocalDateTime requestedAt = LocalDateTime.now();

        passwordResetTokenRepository.deleteByExpiresAtBefore(requestedAt.minusDays(1));

        userRepository.findByEmail(email)
                .filter(User::isActive)
                .ifPresent(user -> {
                    passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId())
                            .forEach(token -> {
                                token.markUsed(requestedAt);
                                passwordResetTokenRepository.save(token);
                            });

                    String rawToken = passwordResetTokenService.generateToken();
                    String tokenHash = passwordResetTokenService.hashToken(rawToken);

                    PasswordResetToken resetToken = new PasswordResetToken(
                            user.getId(),
                            tokenHash,
                            requestedAt.plusMinutes(PASSWORD_RESET_EXPIRATION_MINUTES)
                    );

                    passwordResetTokenRepository.save(resetToken);
                    passwordRecoveryDeliveryPort.sendPasswordResetLink(user, rawToken);
                });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordCommand command) {
        if (command.token() == null || command.token().isBlank()) {
            throw new IllegalArgumentException("Password reset token is required.");
        }

        if (!passwordPolicyService.isValid(command.password())) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        LocalDateTime requestedAt = LocalDateTime.now();
        String tokenHash = passwordResetTokenService.hashToken(command.token().trim());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Password reset token is invalid or expired."));

        if (!resetToken.isUsableAt(requestedAt)) {
            resetToken.markUsed(requestedAt);
            passwordResetTokenRepository.save(resetToken);
            throw new IllegalArgumentException("Password reset token is invalid or expired.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Password reset token is invalid or expired."));

        user.setPasswordHash(hashingService.hash(command.password()));
        userRepository.save(user);

        resetToken.markUsed(requestedAt);
        passwordResetTokenRepository.save(resetToken);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
