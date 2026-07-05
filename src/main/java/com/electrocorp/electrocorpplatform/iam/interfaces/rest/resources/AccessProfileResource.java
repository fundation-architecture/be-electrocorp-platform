package com.electrocorp.electrocorpplatform.iam.interfaces.rest.resources;

import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;

import java.util.Set;

public record AccessProfileResource(
        Long id,
        String name,
        String description,
        Set<AccessPermission> permissions
) {
}
