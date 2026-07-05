package com.electrocorp.electrocorpplatform.iam.application.commandservices;

import com.electrocorp.electrocorpplatform.iam.application.results.AuthenticationResult;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.RecoverPasswordCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.ResetPasswordCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.SignInCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.SignUpCommand;

public interface AuthCommandService {
    AuthenticationResult signUp(SignUpCommand command);

    AuthenticationResult signIn(SignInCommand command);

    void signOut();

    void recoverPassword(RecoverPasswordCommand command);

    void resetPassword(ResetPasswordCommand command);
}
