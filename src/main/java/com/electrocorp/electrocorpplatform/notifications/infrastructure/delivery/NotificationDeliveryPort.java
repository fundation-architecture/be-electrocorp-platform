package com.electrocorp.electrocorpplatform.notifications.infrastructure.delivery;

import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.Alert;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.NotificationPreference;
import com.electrocorp.electrocorpplatform.notifications.domain.model.results.NotificationDeliveryDecision;

public interface NotificationDeliveryPort {

    void send(Alert alert, NotificationPreference preference, NotificationDeliveryDecision decision);
}
