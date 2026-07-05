package com.electrocorp.electrocorpplatform.iam.application.services;

import com.electrocorp.electrocorpplatform.iam.application.commandservices.UserCommandService;
import com.electrocorp.electrocorpplatform.iam.application.queryservices.UserQueryService;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.UpdateProfileCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserApplicationService implements UserCommandService, UserQueryService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User getProfile(Long userId) {
        return findUser(userId);
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UpdateProfileCommand command) {
        User user = findUser(userId);
        user.setFullName(command.fullName());
        user.setEmail(command.email());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        User user = findUser(userId);
        user.deactivate();
        userRepository.save(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }
}
