package com.electrocorp.electrocorpplatform.notifications.application.internal.queryservices;

import com.electrocorp.electrocorpplatform.notifications.application.queryservices.NotificationQueryService;
import com.electrocorp.electrocorpplatform.notifications.application.services.NotificationApplicationService;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.Alert;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.AlertRule;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.AlertRuleProfile;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.NotificationPreference;
import com.electrocorp.electrocorpplatform.notifications.domain.model.queries.EvaluateAlertRulesQuery;
import com.electrocorp.electrocorpplatform.notifications.domain.model.queries.GetAlertRulesByUserQuery;
import com.electrocorp.electrocorpplatform.notifications.domain.model.queries.GetAlertRuleProfilesByUserQuery;
import com.electrocorp.electrocorpplatform.notifications.domain.model.queries.GetAlertsByUserQuery;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.AlertSourceType;
import com.electrocorp.electrocorpplatform.notifications.domain.model.queries.GetNotificationPreferenceQuery;
import com.electrocorp.electrocorpplatform.notifications.domain.model.results.RuleEvaluationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationQueryServiceImpl implements NotificationQueryService {
    private final NotificationApplicationService applicationService;

    @Override
    public List<Alert> handle(GetAlertsByUserQuery query) {
        return applicationService.getAlerts(query.userId()).stream()
                .filter(alert -> query.deviceId() == null
                        || (alert.getSourceType() == AlertSourceType.DEVICE
                        && String.valueOf(query.deviceId()).equals(alert.getSourceId())))
                .filter(alert -> query.sourceType() == null
                        || (alert.getSourceType() != null && alert.getSourceType().name().equalsIgnoreCase(query.sourceType())))
                .filter(alert -> query.eventType() == null
                        || (alert.getEventType() != null && alert.getEventType().name().equalsIgnoreCase(query.eventType())))
                .filter(alert -> query.active() == null || query.active().equals(alert.getActive()))
                .toList();
    }

    @Override
    public List<AlertRule> handle(GetAlertRulesByUserQuery query) {
        return applicationService.getRules(query.userId());
    }

    @Override
    public List<AlertRuleProfile> handle(GetAlertRuleProfilesByUserQuery query) {
        return applicationService.getRuleProfiles(query.userId());
    }

    @Override
    public RuleEvaluationResult handle(EvaluateAlertRulesQuery query) {
        return applicationService.evaluateRules(query.userId(), query.scopeType(), query.scopeId(), query.observedValue());
    }

    @Override
    public NotificationPreference handle(GetNotificationPreferenceQuery query) {
        return applicationService.getPreference(query.userId());
    }
}
