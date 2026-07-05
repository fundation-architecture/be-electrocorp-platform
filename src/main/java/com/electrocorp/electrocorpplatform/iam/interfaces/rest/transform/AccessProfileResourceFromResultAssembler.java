package com.electrocorp.electrocorpplatform.iam.interfaces.rest.transform;

import com.electrocorp.electrocorpplatform.iam.application.results.AccessProfileDetails;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.resources.AccessProfileResource;

public class AccessProfileResourceFromResultAssembler {
    private AccessProfileResourceFromResultAssembler() {
    }

    public static AccessProfileResource toResourceFromResult(AccessProfileDetails details) {
        return new AccessProfileResource(
                details.profile().getId(),
                details.profile().getName(),
                details.profile().getDescription(),
                details.permissions()
        );
    }
}
