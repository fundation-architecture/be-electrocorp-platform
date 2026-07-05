package com.electrocorp.electrocorpplatform.iam.application.queryservices;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;

public interface AuthenticatedUserQueryService {
    User getAuthenticatedUser(Long userId);
}
