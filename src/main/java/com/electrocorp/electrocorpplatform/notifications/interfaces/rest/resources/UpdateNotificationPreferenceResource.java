package com.electrocorp.electrocorpplatform.notifications.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceResource(
        Boolean emailEnabled,
        Boolean pushEnabled,
        Boolean criticalOnly,
        String minimumLevel,
        String scopeType,
        String scopeId,
        Boolean inAppEnabled,
        Boolean toastEnabled,
        Boolean dashboardEnabled,
        String allowedLevels,
        String allowedSourceTypes,
        Boolean quietHoursEnabled,
        String quietHoursStart,
        String quietHoursEnd,
        Boolean criticalBreaksQuietHours,
        Boolean groupSimilarAlerts,
        Boolean remindersEnabled,
        Integer cooldownMinutes,
        Integer maxAlertsPerHour,
        Boolean routineNightSilence,
        Boolean goalDeadlineAlerts,
        Boolean maintenanceDeviceAlerts,
        Boolean systemRecommendations,
        Boolean dailySummaryEnabled,
        Boolean weeklySummaryEnabled,
        Boolean monthlyReportEnabled,
        String defaultDeliveryMode
) {
}
