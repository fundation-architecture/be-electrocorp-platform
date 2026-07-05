package com.electrocorp.electrocorpplatform.shared.interfaces.rest.errors;

import com.electrocorp.electrocorpplatform.shared.application.result.ApplicationError;
import com.electrocorp.electrocorpplatform.shared.interfaces.rest.resources.ErrorResource;

import java.time.LocalDateTime;

public final class ErrorResponseAssembler {

    private ErrorResponseAssembler() {
    }

    public static ErrorResource toResourceFromError(ApplicationError error) {
        return new ErrorResource(error.code(), error.message(), LocalDateTime.now());
    }
}
