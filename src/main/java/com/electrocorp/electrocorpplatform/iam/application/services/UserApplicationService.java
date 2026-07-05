package com.electrocorp.electrocorpplatform.iam.application.services;

import com.electrocorp.electrocorpplatform.iam.application.commandservices.UserCommandService;
import com.electrocorp.electrocorpplatform.iam.application.queryservices.UserQueryService;
import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.AccessProfile;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.AssignAccessProfileCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.UpdateProfileCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.UpdateUiPreferenceCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.UserUiPreference;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.AccessProfileRepository;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.UserRepository;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.UserUiPreferenceRepository;
import com.electrocorp.electrocorpplatform.iam.domain.services.AccessProfilePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserApplicationService implements UserCommandService, UserQueryService {

    private final UserRepository userRepository;
    private final AccessProfileRepository accessProfileRepository;
    private final UserUiPreferenceRepository userUiPreferenceRepository;
    private final AccessProfilePolicyService accessProfilePolicyService;

    @Override
    @Transactional(readOnly = true)
    public User getProfile(Long userId) {
        return findUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsers() {
        return userRepository.findAll()
                .stream()
                .filter(User::isActive)
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional
    public UserUiPreference getUiPreference(Long userId) {
        findUser(userId);
        return userUiPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> userUiPreferenceRepository.save(new UserUiPreference(userId)));
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
    public User assignAccessProfile(AssignAccessProfileCommand command) {
        User actor = findUser(command.actorUserId());

        if (!accessProfilePolicyService.hasPermission(actor.getAccessProfile(), AccessPermission.MANAGE_ACCESS)) {
            throw new AccessDeniedException("Current user cannot manage access profiles.");
        }

        User target = findUser(command.targetUserId());
        AccessProfile profile = accessProfileRepository.findById(command.accessProfileId())
                .orElseThrow(() -> new IllegalArgumentException("Access profile not found."));

        target.setAccessProfile(profile);

        return userRepository.save(target);
    }

    @Override
    @Transactional
    public UserUiPreference updateUiPreference(Long userId, UpdateUiPreferenceCommand command) {
        findUser(userId);
        UserUiPreference preference = userUiPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> new UserUiPreference(userId));
        preference.update(command.language(), command.theme());
        return userUiPreferenceRepository.save(preference);
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
