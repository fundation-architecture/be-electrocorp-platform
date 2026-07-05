package com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates;

import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.NotificationDeliveryMode;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.NotificationPreferenceScope;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.QuietHours;
import com.electrocorp.electrocorpplatform.shared.domain.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotificationPreferenceScope scopeType = NotificationPreferenceScope.USER;

    @Column(length = 80)
    private String scopeId;

    @Column(nullable = false)
    private Boolean emailEnabled = true;

    @Column(nullable = false)
    private Boolean pushEnabled = true;

    @Column
    private Boolean inAppEnabled = true;

    @Column
    private Boolean toastEnabled = true;

    @Column
    private Boolean dashboardEnabled = true;

    @Column(nullable = false)
    private Boolean criticalOnly = false;

    @Column(nullable = false, length = 30)
    private String minimumLevel = "INFO";

    @Column(length = 120)
    private String allowedLevels = "STABLE,INFO,WARNING,CRITICAL,SUCCESS";

    @Column(length = 220)
    private String allowedSourceTypes = "DEVICE,GROUP,ROOM,WORKPLACE,ROUTINE,GOAL,REPORT,RULE,MODE,SYSTEM";

    @Embedded
    private QuietHours quietHours = new QuietHours(false, "22:00", "07:00");

    @Column
    private Boolean criticalBreaksQuietHours = true;

    @Column
    private Boolean groupSimilarAlerts = true;

    @Column
    private Boolean remindersEnabled = true;

    @Column
    private Integer cooldownMinutes = 10;

    @Column
    private Integer maxAlertsPerHour = 12;

    @Column
    private Boolean routineNightSilence = true;

    @Column
    private Boolean goalDeadlineAlerts = true;

    @Column
    private Boolean maintenanceDeviceAlerts = false;

    @Column
    private Boolean systemRecommendations = true;

    @Column
    private Boolean dailySummaryEnabled = true;

    @Column
    private Boolean weeklySummaryEnabled = false;

    @Column
    private Boolean monthlyReportEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotificationDeliveryMode defaultDeliveryMode = NotificationDeliveryMode.BANNER;

    public void applyNoiseSettings(
            Boolean inAppEnabled,
            Boolean toastEnabled,
            Boolean dashboardEnabled,
            String allowedLevels,
            String allowedSourceTypes,
            QuietHours quietHours,
            Boolean criticalBreaksQuietHours,
            Boolean groupSimilarAlerts,
            Boolean remindersEnabled,
            Integer cooldownMinutes,
            Integer maxAlertsPerHour,
            NotificationDeliveryMode defaultDeliveryMode
    ) {
        if (inAppEnabled != null) this.inAppEnabled = inAppEnabled;
        if (toastEnabled != null) this.toastEnabled = toastEnabled;
        if (dashboardEnabled != null) this.dashboardEnabled = dashboardEnabled;
        if (allowedLevels != null && !allowedLevels.isBlank()) this.allowedLevels = allowedLevels;
        if (allowedSourceTypes != null && !allowedSourceTypes.isBlank()) this.allowedSourceTypes = allowedSourceTypes;
        if (quietHours != null) this.quietHours = quietHours;
        if (criticalBreaksQuietHours != null) this.criticalBreaksQuietHours = criticalBreaksQuietHours;
        if (groupSimilarAlerts != null) this.groupSimilarAlerts = groupSimilarAlerts;
        if (remindersEnabled != null) this.remindersEnabled = remindersEnabled;
        if (cooldownMinutes != null) this.cooldownMinutes = Math.max(0, Math.min(1440, cooldownMinutes));
        if (maxAlertsPerHour != null) this.maxAlertsPerHour = Math.max(1, Math.min(200, maxAlertsPerHour));
        if (defaultDeliveryMode != null) this.defaultDeliveryMode = defaultDeliveryMode;
    }
}
