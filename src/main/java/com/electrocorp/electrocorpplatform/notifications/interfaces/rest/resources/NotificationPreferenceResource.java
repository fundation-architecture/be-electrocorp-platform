package com.electrocorp.electrocorpplatform.notifications.interfaces.rest.resources;

import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.NotificationPreference;

public record NotificationPreferenceResource(
        Long id,
        Long userId,
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
    public static NotificationPreferenceResource from(NotificationPreference preference) {
        return new NotificationPreferenceResource(
                preference.getId(),
                preference.getUserId(),
                preference.getEmailEnabled(),
                preference.getPushEnabled(),
                preference.getCriticalOnly(),
                preference.getMinimumLevel(),
                preference.getScopeType() == null ? null : preference.getScopeType().name(),
                preference.getScopeId(),
                preference.getInAppEnabled(),
                preference.getToastEnabled(),
                preference.getDashboardEnabled(),
                preference.getAllowedLevels(),
                preference.getAllowedSourceTypes(),
                preference.getQuietHours() == null ? false : preference.getQuietHours().getEnabled(),
                preference.getQuietHours() == null ? null : preference.getQuietHours().getStartTime(),
                preference.getQuietHours() == null ? null : preference.getQuietHours().getEndTime(),
                preference.getCriticalBreaksQuietHours(),
                preference.getGroupSimilarAlerts(),
                preference.getRemindersEnabled(),
                preference.getCooldownMinutes(),
                preference.getMaxAlertsPerHour(),
                preference.getRoutineNightSilence(),
                preference.getGoalDeadlineAlerts(),
                preference.getMaintenanceDeviceAlerts(),
                preference.getSystemRecommendations(),
                preference.getDailySummaryEnabled(),
                preference.getWeeklySummaryEnabled(),
                preference.getMonthlyReportEnabled(),
                preference.getDefaultDeliveryMode() == null ? null : preference.getDefaultDeliveryMode().name()
        );
    }
}
