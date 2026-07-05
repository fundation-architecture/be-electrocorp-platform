package com.electrocorp.electrocorpplatform.iam.interfaces.rest.transform;

import com.electrocorp.electrocorpplatform.iam.domain.model.commands.ResetPasswordCommand;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.resources.ResetPasswordResource;

public class ResetPasswordCommandFromResourceAssembler {

    private ResetPasswordCommandFromResourceAssembler() {
    }

    public static ResetPasswordCommand toCommandFromResource(ResetPasswordResource resource) {
        return new ResetPasswordCommand(resource.token(), resource.password());
    }
}
