package com.electrocorp.electrocorpplatform.iam.application.security;

import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.AccessProfile;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.UserRepository;
import com.electrocorp.electrocorpplatform.iam.domain.services.AccessProfilePolicyService;
import com.electrocorp.electrocorpplatform.shared.infrastructure.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessAuthorizationServiceTest {

    @Test
    void ownerCanPassRequiredPermission() {
        TestContext context = new TestContext();
        User owner = activeUser("OWNER");

        when(context.currentUserProvider.getCurrentUserId()).thenReturn(10L);
        when(context.userRepository.findById(10L)).thenReturn(Optional.of(owner));

        Long userId = context.service.requirePermission(AccessPermission.MANAGE_BILLING);

        assertEquals(10L, userId);
    }

    @Test
    void activeUserCanPassWithoutModulePermission() {
        TestContext context = new TestContext();
        User guest = activeUser("GUEST");

        when(context.currentUserProvider.getCurrentUserId()).thenReturn(13L);
        when(context.userRepository.findById(13L)).thenReturn(Optional.of(guest));

        Long userId = context.service.requireActiveUser();

        assertEquals(13L, userId);
    }

    @Test
    void guestCannotPassManagementPermission() {
        TestContext context = new TestContext();
        User guest = activeUser("GUEST");

        when(context.currentUserProvider.getCurrentUserId()).thenReturn(11L);
        when(context.userRepository.findById(11L)).thenReturn(Optional.of(guest));

        assertThrows(
                AccessDeniedException.class,
                () -> context.service.requirePermission(AccessPermission.MANAGE_ACCESS)
        );
    }

    @Test
    void currentUserCanReadOwnAccountWithoutManagementPermission() {
        TestContext context = new TestContext();
        User guest = activeUser("GUEST");

        when(context.currentUserProvider.getCurrentUserId()).thenReturn(12L);
        when(context.userRepository.findById(12L)).thenReturn(Optional.of(guest));

        Long userId = context.service.requireSelfOrPermission(12L, AccessPermission.MANAGE_ACCESS);

        assertEquals(12L, userId);
    }

    private static User activeUser(String profileName) {
        AccessProfile profile = mock(AccessProfile.class);
        User user = mock(User.class);

        when(profile.getName()).thenReturn(profileName);
        when(user.getAccessProfile()).thenReturn(profile);
        when(user.isActive()).thenReturn(true);

        return user;
    }

    private static class TestContext {
        private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        private final UserRepository userRepository = mock(UserRepository.class);
        private final AccessAuthorizationService service = new AccessAuthorizationService(
                currentUserProvider,
                userRepository,
                new AccessProfilePolicyService()
        );
    }
}
