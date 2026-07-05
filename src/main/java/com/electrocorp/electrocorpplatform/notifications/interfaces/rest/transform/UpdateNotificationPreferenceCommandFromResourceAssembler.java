package com.electrocorp.electrocorpplatform.notifications.interfaces.rest.transform;

import com.electrocorp.electrocorpplatform.notifications.domain.model.commands.UpdateNotificationPreferenceCommand;
import com.electrocorp.electrocorpplatform.notifications.interfaces.rest.resources.UpdateNotificationPreferenceResource;

public class UpdateNotificationPreferenceCommandFromResourceAssembler {
    public static UpdateNotificationPreferenceCommand toCommandFromResource(UpdateNotificationPreferenceResource resource, Long userId) {
        return new UpdateNotificationPreferenceCommand(
                userId,
                resource.emailEnabled(),
                resource.pushEnabled(),
                resource.criticalOnly(),
                resource.minimumLevel(),
                resource.scopeType(),
                resource.scopeId(),
                resource.inAppEnabled(),
                resource.toastEnabled(),
                resource.dashboardEnabled(),
                resource.allowedLevels(),
                resource.allowedSourceTypes(),
                resource.quietHoursEnabled(),
                resource.quietHoursStart(),
                resource.quietHoursEnd(),
                resource.criticalBreaksQuietHours(),
                resource.groupSimilarAlerts(),
                resource.remindersEnabled(),
                resource.cooldownMinutes(),
                resource.maxAlertsPerHour(),
                resource.routineNightSilence(),
                resource.goalDeadlineAlerts(),
                resource.maintenanceDeviceAlerts(),
                resource.systemRecommendations(),
                resource.dailySummaryEnabled(),
                resource.weeklySummaryEnabled(),
                resource.monthlyReportEnabled(),
                resource.defaultDeliveryMode()
        );
    }
}
