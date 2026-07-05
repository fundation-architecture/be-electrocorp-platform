package com.electrocorp.electrocorpplatform.iam.application.services;

import com.electrocorp.electrocorpplatform.iam.application.commandservices.AuthCommandService;
import com.electrocorp.electrocorpplatform.iam.application.queryservices.AuthenticatedUserQueryService;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.RecoverPasswordCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.SignInCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.SignUpCommand;
import com.electrocorp.electrocorpplatform.iam.application.results.AuthenticationResult;
import com.electrocorp.electrocorpplatform.iam.application.security.HashingService;
import com.electrocorp.electrocorpplatform.iam.application.security.TokenService;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.AccessProfile;
import com.electrocorp.electrocorpplatform.iam.domain.model.AccountStatus;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;
import com.electrocorp.electrocorpplatform.iam.domain.model.events.UserRegisteredEvent;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.AccessProfileRepository;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.UserRepository;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthApplicationService implements AuthCommandService, AuthenticatedUserQueryService {

    private final UserRepository userRepository;
    private final AccessProfileRepository accessProfileRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional
    public AuthenticationResult signUp(SignUpCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Email already exists.");
        }

        AccessProfile profile = accessProfileRepository.findByName("MEMBER")
                .orElseGet(() -> accessProfileRepository.save(new AccessProfile("MEMBER", "Default user profile")));

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
    }
}
