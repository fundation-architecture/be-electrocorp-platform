package com.electrocorp.electrocorpplatform.iam.application.security;

import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.UserRepository;
import com.electrocorp.electrocorpplatform.iam.domain.services.AccessProfilePolicyService;
import com.electrocorp.electrocorpplatform.shared.infrastructure.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessAuthorizationService {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final AccessProfilePolicyService accessProfilePolicyService;

    @Transactional(readOnly = true)
    public Long requireActiveUser() {
        Long userId = currentUserProvider.getCurrentUserId();
        findActiveCurrentUser(userId);
        return userId;
    }

    @Transactional(readOnly = true)
    public Long requirePermission(AccessPermission permission) {
        Long userId = currentUserProvider.getCurrentUserId();
        User user = findActiveCurrentUser(userId);

        if (!accessProfilePolicyService.hasPermission(user.getAccessProfile(), permission)) {
            throw new AccessDeniedException("Current user does not have permission: " + permission.name());
        }

        return userId;
    }

    @Transactional(readOnly = true)
    public Long requireSelfOrPermission(Long targetUserId, AccessPermission permission) {
        Long userId = currentUserProvider.getCurrentUserId();
        User user = findActiveCurrentUser(userId);

        if (userId.equals(targetUserId)) {
            return userId;
        }

        if (!accessProfilePolicyService.hasPermission(user.getAccessProfile(), permission)) {
            throw new AccessDeniedException("Current user cannot access another user account.");
        }

        return userId;
    }

    private User findActiveCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user was not found."));

        if (!user.isActive()) {
            throw new AccessDeniedException("Authenticated user is inactive.");
        }

        return user;
    }
}
