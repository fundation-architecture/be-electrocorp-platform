package com.electrocorp.electrocorpplatform.iam.application.commandservices;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.UserUiPreference;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.AssignAccessProfileCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.UpdateProfileCommand;
import com.electrocorp.electrocorpplatform.iam.domain.model.commands.UpdateUiPreferenceCommand;

public interface UserCommandService {
    User updateProfile(Long userId, UpdateProfileCommand command);

    User assignAccessProfile(AssignAccessProfileCommand command);

    UserUiPreference updateUiPreference(Long userId, UpdateUiPreferenceCommand command);

    void deleteAccount(Long userId);
}
