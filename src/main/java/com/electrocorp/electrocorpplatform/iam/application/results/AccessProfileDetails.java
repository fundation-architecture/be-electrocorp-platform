package com.electrocorp.electrocorpplatform.iam.application.results;

import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.AccessProfile;

import java.util.Set;

public record AccessProfileDetails(
        AccessProfile profile,
        Set<AccessPermission> permissions
) {
}
