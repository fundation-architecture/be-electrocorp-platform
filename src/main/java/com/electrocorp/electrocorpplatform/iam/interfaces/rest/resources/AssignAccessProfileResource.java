package com.electrocorp.electrocorpplatform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

public record AssignAccessProfileResource(
        @NotNull Long accessProfileId
) {
}
