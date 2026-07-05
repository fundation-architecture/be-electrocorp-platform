package com.electrocorp.electrocorpplatform.iam.interfaces.rest.controllers;

import com.electrocorp.electrocorpplatform.iam.application.commandservices.AuthCommandService;
import com.electrocorp.electrocorpplatform.iam.application.queryservices.AuthenticatedUserQueryService;
import com.electrocorp.electrocorpplatform.shared.infrastructure.security.CurrentUserProvider;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.resources.*;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.transform.AuthResourceFromResultAssembler;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.transform.RecoverPasswordCommandFromResourceAssembler;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.transform.ResetPasswordCommandFromResourceAssembler;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthCommandService authCommandService;
    private final AuthenticatedUserQueryService authenticatedUserQueryService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/sign-up")
    public AuthResource signUp(@Valid @RequestBody SignUpResource request) {
        return AuthResourceFromResultAssembler.toResourceFromResult(
                authCommandService.signUp(SignUpCommandFromResourceAssembler.toCommandFromResource(request))
        );
    }

    @PostMapping("/sign-in")
    public AuthResource signIn(@Valid @RequestBody SignInResource request) {
        return AuthResourceFromResultAssembler.toResourceFromResult(
                authCommandService.signIn(SignInCommandFromResourceAssembler.toCommandFromResource(request))
        );
    }

    @PostMapping("/sign-out")
    public void signOut() {
        authCommandService.signOut();
    }

    @PostMapping("/recover-password")
    public void recoverPassword(@Valid @RequestBody RecoverPasswordResource request) {
        authCommandService.recoverPassword(RecoverPasswordCommandFromResourceAssembler.toCommandFromResource(request));
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordResource request) {
        authCommandService.resetPassword(ResetPasswordCommandFromResourceAssembler.toCommandFromResource(request));
    }

    @GetMapping("/me")
    public UserResource me() {
        return UserResourceFromEntityAssembler.toResourceFromEntity(
                authenticatedUserQueryService.getAuthenticatedUser(currentUserProvider.getCurrentUserId())
        );
    }
}
