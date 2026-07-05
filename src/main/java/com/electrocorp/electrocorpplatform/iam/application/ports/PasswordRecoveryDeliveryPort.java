package com.electrocorp.electrocorpplatform.iam.application.ports;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;

public interface PasswordRecoveryDeliveryPort {
    void sendPasswordResetLink(User user, String resetToken);
}
