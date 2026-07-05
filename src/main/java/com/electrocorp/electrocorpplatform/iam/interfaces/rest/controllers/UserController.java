package com.electrocorp.electrocorpplatform.iam.interfaces.rest.controllers;

import com.electrocorp.electrocorpplatform.iam.application.commandservices.UserCommandService;
import com.electrocorp.electrocorpplatform.iam.application.queryservices.UserQueryService;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.resources.UpdateProfileResource;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.resources.UserResource;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.transform.UpdateProfileCommandFromResourceAssembler;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import com.electrocorp.electrocorpplatform.shared.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/me")
    public UserResource getCurrentUserProfile() {
        return UserResourceFromEntityAssembler.toResourceFromEntity(
                userQueryService.getProfile(currentUserProvider.getCurrentUserId())
        );
    }

    @PutMapping("/me")
    public UserResource updateCurrentUserProfile(@Valid @RequestBody UpdateProfileResource request) {
        return UserResourceFromEntityAssembler.toResourceFromEntity(
                userCommandService.updateProfile(
                        currentUserProvider.getCurrentUserId(),
                        UpdateProfileCommandFromResourceAssembler.toCommandFromResource(request)
                )
        );
    }

    @DeleteMapping("/me")
    public void deleteCurrentUserAccount() {
        userCommandService.deleteAccount(currentUserProvider.getCurrentUserId());
    }

    @GetMapping("/{userId}/profile")
    public UserResource getProfile(@PathVariable Long userId) {
        return UserResourceFromEntityAssembler.toResourceFromEntity(userQueryService.getProfile(userId));
    }

    @PutMapping("/{userId}/profile")
    public UserResource updateProfile(@PathVariable Long userId, @Valid @RequestBody UpdateProfileResource request) {
        return UserResourceFromEntityAssembler.toResourceFromEntity(
                userCommandService.updateProfile(
                        userId,
                        UpdateProfileCommandFromResourceAssembler.toCommandFromResource(request)
                )
        );
    }

    @DeleteMapping("/{userId}")
    public void deleteAccount(@PathVariable Long userId) {
        userCommandService.deleteAccount(userId);
    }
}
