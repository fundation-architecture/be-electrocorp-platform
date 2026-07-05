package com.electrocorp.electrocorpplatform.notifications.application.services;

import com.electrocorp.electrocorpplatform.notifications.domain.model.commands.*;
import com.electrocorp.electrocorpplatform.notifications.domain.factories.AlertFactory;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.*;
import com.electrocorp.electrocorpplatform.notifications.domain.model.results.RuleEvaluationResult;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.AlertEventType;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.AlertSourceType;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.RuleEvaluatorType;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.RuleProfileMode;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.RuleScopeType;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.RuleSensitivity;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.NotificationDeliveryMode;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.NotificationPreferenceScope;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.QuietHours;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.*;
import com.electrocorp.electrocorpplatform.notifications.domain.services.AlertPolicyService;
import com.electrocorp.electrocorpplatform.notifications.domain.services.AlertSourceOwnershipService;
import com.electrocorp.electrocorpplatform.notifications.domain.services.NotificationDeliveryPolicyService;
import com.electrocorp.electrocorpplatform.notifications.domain.services.NotificationPreferencePolicyService;
import com.electrocorp.electrocorpplatform.notifications.domain.services.RuleEvaluationService;
import com.electrocorp.electrocorpplatform.notifications.infrastructure.delivery.NotificationDeliveryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NotificationApplicationService {

    private final AlertRepository alertRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertRuleProfileRepository alertRuleProfileRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final AlertPolicyService alertPolicyService;
    private final AlertSourceOwnershipService alertSourceOwnershipService;
    private final RuleEvaluationService ruleEvaluationService;
    private final NotificationPreferencePolicyService notificationPreferencePolicyService;
    private final NotificationDeliveryPolicyService notificationDeliveryPolicyService;
    private final NotificationDeliveryPort notificationDeliveryPort;
    private final AlertFactory alertFactory = new AlertFactory();

    @Transactional
    public List<Alert> getAlerts(Long userId) {
        List<Alert> alerts = alertRepository.findByUserId(userId);

        for (Alert alert : alerts) {
            if (Boolean.TRUE.equals(alert.getActive()) && alert.isExpired()) {
                alert.expire();
                alertRepository.save(alert);
            }
        }

        return alerts;
    }

    @Transactional
    public Alert createAlert(CreateAlertCommand command) {
        AlertSourceType sourceType = alertPolicyService.parseSourceType(command.sourceType());
        AlertEventType eventType = alertPolicyService.parseEventType(command.eventType());
        alertPolicyService.parseLevel(command.level());
        alertSourceOwnershipService.validateSourceOwnership(command.userId(), sourceType, command.sourceId());

        String threadKey = buildThreadKey(command, sourceType, eventType);
        LocalDateTime expiresAt = parseOptionalDateTime(command.expiresAt());
        LocalDateTime now = LocalDateTime.now();

        Alert alert = alertRepository
                .findFirstByUserIdAndThreadKeyAndActiveTrueOrderByLastTriggeredAtDesc(command.userId(), threadKey)
                .map(existingAlert -> {
                    existingAlert.refresh(
                            command.title(),
                            command.message(),
                            command.level(),
                            command.evidence(),
                            command.explanation(),
                            command.recommendedAction(),
                            alertPolicyService.normalizeScore(command.severityScore()),
                            now
                    );
                    existingAlert.setExpiresAt(expiresAt);
                    existingAlert.setSourceLabel(normalizeOptional(command.sourceLabel()));
                    return alertRepository.save(existingAlert);
                })
                .orElseGet(() -> {
                    Alert createdAlert = alertFactory.create(
                            command.userId(),
                            command.title(),
                            command.message(),
                            command.level(),
                            sourceType,
                            command.sourceId(),
                            command.sourceLabel(),
                            eventType,
                            threadKey,
                            command.evidence(),
                            command.explanation(),
                            command.recommendedAction(),
                            alertPolicyService.normalizeScore(command.severityScore()),
                            expiresAt
                    );
                    return alertRepository.save(createdAlert);
                });

        deliverAlert(alert);
        return alert;
    }

    private void deliverAlert(Alert alert) {
        NotificationPreference preference = getPreference(alert.getUserId());
        var decision = notificationDeliveryPolicyService.decide(preference, alert, LocalTime.now());

        if (decision.allowed()) {
            notificationDeliveryPort.send(alert, preference, decision);
        }
    }

    @Transactional
    public Alert markAsRead(Long userId, Long alertId) {
        Alert alert = alertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found."));
        alert.markAsRead();
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert dismissAlert(Long userId, Long alertId, Integer minutes) {
        int safeMinutes = minutes == null ? 10 : Math.max(1, Math.min(1440, minutes));
        Alert alert = alertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found."));
        alert.dismissUntil(LocalDateTime.now().plusMinutes(safeMinutes));
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert resolveAlert(Long userId, Long alertId) {
        Alert alert = alertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found."));
        alert.resolve();
        return alertRepository.save(alert);
    }

    @Transactional
    public void deleteAlert(Long userId, Long alertId) {
        Alert alert = alertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found."));
        alertRepository.delete(alert);
    }

    private String buildThreadKey(CreateAlertCommand command, AlertSourceType sourceType, AlertEventType eventType) {
        if (command.threadKey() != null && !command.threadKey().isBlank()) {
            return command.threadKey().trim().toUpperCase(Locale.ROOT);
        }

        String sourceId = command.sourceId() == null || command.sourceId().isBlank()
                ? "GLOBAL"
                : command.sourceId().trim();

        return "%s:%s:%s".formatted(sourceType.name(), sourceId, eventType.name());
    }

    private LocalDateTime parseOptionalDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Alert expiration date is invalid.");
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateRuleScope(Long userId, RuleScopeType scopeType, String scopeId) {
        if (scopeType == null || scopeType == RuleScopeType.GENERAL || scopeId == null || scopeId.isBlank()) {
            return;
        }

        AlertSourceType sourceType = switch (scopeType) {
            case WORKPLACE -> AlertSourceType.WORKPLACE;
            case ROOM -> AlertSourceType.ROOM;
            case DEVICE -> AlertSourceType.DEVICE;
            case GROUP -> AlertSourceType.GROUP;
            case ROUTINE -> AlertSourceType.ROUTINE;
            case GOAL -> AlertSourceType.GOAL;
            default -> null;
        };

        if (sourceType != null) {
            alertSourceOwnershipService.validateSourceOwnership(userId, sourceType, scopeId);
        }
    }

    private <T extends Enum<T>> T parseEnum(String value, T defaultValue, Class<T> enumType, String errorMessage) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @Transactional(readOnly = true)
    public List<AlertRule> getRules(Long userId) {
        return alertRuleRepository.findByUserId(userId);
    }

    @Transactional
    public AlertRule createRule(CreateAlertRuleCommand command) {
        AlertRule rule = new AlertRule();
        rule.setUserId(command.userId());
        rule.setName(command.name());
        rule.setMetric(command.metric());
        rule.setConditionType(command.conditionType());
        rule.setThreshold(command.threshold());
        rule.setLevel(alertPolicyService.parseLevel(command.level() == null ? "WARNING" : command.level()));
        RuleScopeType scopeType = parseEnum(command.scopeType(), RuleScopeType.GENERAL, RuleScopeType.class, "Rule scope type is invalid.");
        validateRuleScope(command.userId(), scopeType, command.scopeId());
        rule.setScopeType(scopeType);
        rule.setScopeId(normalizeOptional(command.scopeId()));
        rule.setEvaluatorType(parseEnum(command.evaluatorType(), RuleEvaluatorType.ACTIVE_POWER, RuleEvaluatorType.class, "Rule evaluator type is invalid."));
        rule.setWeight(Math.max(0, command.weight() == null ? 10 : command.weight()));
        rule.setProfileName(command.profileName() == null || command.profileName().isBlank() ? "General" : command.profileName().trim());
        rule.setEnabled(true);
        return alertRuleRepository.save(rule);
    }

    @Transactional
    public AlertRule toggleRule(Long userId, Long ruleId) {
        AlertRule rule = alertRuleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Alert rule not found."));
        rule.toggle();
        return alertRuleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        AlertRule rule = alertRuleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Alert rule not found."));
        alertRuleRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public List<AlertRuleProfile> getRuleProfiles(Long userId) {
        return alertRuleProfileRepository.findByUserId(userId);
    }

    @Transactional
    public AlertRuleProfile createRuleProfile(CreateAlertRuleProfileCommand command) {
        AlertRuleProfile profile = new AlertRuleProfile();
        profile.setUserId(command.userId());
        profile.setName(command.name());
        profile.setDescription(command.description() == null || command.description().isBlank()
                ? "Perfil de clasificacion energetica."
                : command.description().trim());
        RuleScopeType scopeType = parseEnum(command.scopeType(), RuleScopeType.GENERAL, RuleScopeType.class, "Rule profile scope type is invalid.");
        validateRuleScope(command.userId(), scopeType, command.scopeId());
        profile.setScopeType(scopeType);
        profile.setScopeId(normalizeOptional(command.scopeId()));
        profile.setMode(parseEnum(command.mode(), RuleProfileMode.BALANCED, RuleProfileMode.class, "Rule profile mode is invalid."));
        profile.setSensitivity(parseEnum(command.sensitivity(), RuleSensitivity.NORMAL, RuleSensitivity.class, "Rule profile sensitivity is invalid."));
        profile.setActive(false);
        return alertRuleProfileRepository.save(profile);
    }

    @Transactional
    public AlertRuleProfile activateRuleProfile(Long userId, Long profileId) {
        List<AlertRuleProfile> profiles = alertRuleProfileRepository.findByUserId(userId);
        AlertRuleProfile selected = profiles.stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Alert rule profile not found."));

        for (AlertRuleProfile profile : profiles) {
            if (profile.getId().equals(profileId)) {
                profile.activate();
            } else {
                profile.deactivate();
            }
            alertRuleProfileRepository.save(profile);
        }

        return selected;
    }

    @Transactional(readOnly = true)
    public RuleEvaluationResult evaluateRules(Long userId, String scopeType, String scopeId, BigDecimal observedValue) {
        RuleScopeType parsedScope = parseEnum(scopeType, RuleScopeType.GENERAL, RuleScopeType.class, "Rule evaluation scope type is invalid.");
        validateRuleScope(userId, parsedScope, scopeId);
        return ruleEvaluationService.evaluate(userId, parsedScope, scopeId, observedValue);
    }

    @Transactional
    public Alert createAlertFromRuleEvaluation(
            Long userId,
            RuleScopeType scopeType,
            String scopeId,
            BigDecimal observedValue,
            String sourceLabel
    ) {
        RuleEvaluationResult result = ruleEvaluationService.evaluate(userId, scopeType, scopeId, observedValue);

        if (result.activeEvaluatorCount() == null || result.activeEvaluatorCount() <= 0) {
            return null;
        }

        String sourceName = sourceLabel == null || sourceLabel.isBlank()
                ? result.sourceType().name()
                : sourceLabel.trim();

        return createAlert(new CreateAlertCommand(
                userId,
                "Evaluacion automatica de reglas",
                "%s clasificado como %s con score %d.".formatted(
                        sourceName,
                        result.level().name(),
                        result.severityScore()
                ),
                result.level().name(),
                result.sourceType().name(),
                result.sourceId(),
                sourceName,
                result.eventType().name(),
                result.threadKey(),
                result.evidence(),
                result.explanation(),
                result.recommendedAction(),
                result.severityScore(),
                LocalDateTime.now().plusHours(2).toString()
        ));
    }

    @Transactional
    public NotificationPreference getPreference(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference created = new NotificationPreference();
                    created.setUserId(userId);
                    return preferenceRepository.save(created);
                });
    }

    @Transactional
    public NotificationPreference updatePreference(UpdateNotificationPreferenceCommand command) {
        NotificationPreference preference = preferenceRepository.findByUserId(command.userId())
                .orElseGet(() -> {
                    NotificationPreference created = new NotificationPreference();
                    created.setUserId(command.userId());
                    return created;
                });

        if (command.emailEnabled() != null) preference.setEmailEnabled(command.emailEnabled());
        if (command.pushEnabled() != null) preference.setPushEnabled(command.pushEnabled());
        if (command.criticalOnly() != null) preference.setCriticalOnly(command.criticalOnly());
        if (command.minimumLevel() != null) {
            notificationPreferencePolicyService.validatePreference(command.minimumLevel());
            preference.setMinimumLevel(command.minimumLevel().trim().toUpperCase(Locale.ROOT));
        }
        if (command.scopeType() != null) {
            preference.setScopeType(parseEnum(command.scopeType(), NotificationPreferenceScope.USER, NotificationPreferenceScope.class, "Notification preference scope is invalid."));
        }
        if (command.scopeId() != null) preference.setScopeId(normalizeOptional(command.scopeId()));
        if (command.routineNightSilence() != null) preference.setRoutineNightSilence(command.routineNightSilence());
        if (command.goalDeadlineAlerts() != null) preference.setGoalDeadlineAlerts(command.goalDeadlineAlerts());
        if (command.maintenanceDeviceAlerts() != null) preference.setMaintenanceDeviceAlerts(command.maintenanceDeviceAlerts());
        if (command.systemRecommendations() != null) preference.setSystemRecommendations(command.systemRecommendations());
        if (command.dailySummaryEnabled() != null) preference.setDailySummaryEnabled(command.dailySummaryEnabled());
        if (command.weeklySummaryEnabled() != null) preference.setWeeklySummaryEnabled(command.weeklySummaryEnabled());
        if (command.monthlyReportEnabled() != null) preference.setMonthlyReportEnabled(command.monthlyReportEnabled());

        String allowedLevels = notificationPreferencePolicyService.normalizeLevelSet(command.allowedLevels());
        String allowedSources = notificationPreferencePolicyService.normalizeSourceSet(command.allowedSourceTypes());
        QuietHours quietHours = command.quietHoursEnabled() == null
                && command.quietHoursStart() == null
                && command.quietHoursEnd() == null
                ? null
                : new QuietHours(
                        command.quietHoursEnabled() == null && preference.getQuietHours() != null
                                ? preference.getQuietHours().getEnabled()
                                : command.quietHoursEnabled(),
                        command.quietHoursStart() == null && preference.getQuietHours() != null
                                ? preference.getQuietHours().getStartTime()
                                : command.quietHoursStart(),
                        command.quietHoursEnd() == null && preference.getQuietHours() != null
                                ? preference.getQuietHours().getEndTime()
                                : command.quietHoursEnd()
                );
        NotificationDeliveryMode deliveryMode = parseEnum(command.defaultDeliveryMode(), null, NotificationDeliveryMode.class, "Notification delivery mode is invalid.");

        preference.applyNoiseSettings(
                command.inAppEnabled(),
                command.toastEnabled(),
                command.dashboardEnabled(),
                allowedLevels,
                allowedSources,
                quietHours,
                command.criticalBreaksQuietHours(),
                command.groupSimilarAlerts(),
                command.remindersEnabled(),
                command.cooldownMinutes(),
                command.maxAlertsPerHour(),
                deliveryMode
        );

        return preferenceRepository.save(preference);
    }
}
