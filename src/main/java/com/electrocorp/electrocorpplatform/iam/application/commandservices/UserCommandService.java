package com.electrocorp.electrocorpplatform.iam.application.commandservices;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.UpdateProfileCommand;

public interface UserCommandService {
    User updateProfile(Long userId, UpdateProfileCommand command);

    void deleteAccount(Long userId);
}
