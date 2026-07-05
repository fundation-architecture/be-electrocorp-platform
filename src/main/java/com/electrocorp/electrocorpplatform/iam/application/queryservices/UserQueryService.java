package com.electrocorp.electrocorpplatform.iam.application.queryservices;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.UserUiPreference;

import java.util.List;

public interface UserQueryService {
    User getProfile(Long userId);

    List<User> getUsers();

    UserUiPreference getUiPreference(Long userId);
}
