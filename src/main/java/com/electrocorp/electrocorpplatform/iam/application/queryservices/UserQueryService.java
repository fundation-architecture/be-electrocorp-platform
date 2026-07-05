package com.electrocorp.electrocorpplatform.iam.application.queryservices;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;

public interface UserQueryService {
    User getProfile(Long userId);
}
