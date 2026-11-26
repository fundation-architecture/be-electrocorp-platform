package com.electrocorp.electrocorpplatform.notifications.domain.services;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class AlertRulePolicyService {

    private static final Set<String> ALLOWED_METRICS = Set.of(
            "WATTS",
            "DAILY_CONSUMPTION",
            "MONTHLY_CONSUMPTION"
    );

    private static final Set<String> ALLOWED_CONDITIONS = Set.of(
            "GREATER_THAN",
            "GREATER_OR_EQUAL_THAN",
            "LESS_THAN",
            "EQUALS"
    );

    public void validateRule(String name, String metric, String conditionType, BigDecimal threshold) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Alert rule name is required.");
        }

        if (metric == null || !ALLOWED_METRICS.contains(metric.toUpperCase())) {
            throw new IllegalArgumentException("Alert rule metric is invalid.");
        }

        if (conditionType == null || !ALLOWED_CONDITIONS.contains(conditionType.toUpperCase())) {
            throw new IllegalArgumentException("Alert rule condition is invalid.");
        }

        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Alert rule threshold must be zero or greater.");
        }

        if ("WATTS".equalsIgnoreCase(metric) && threshold.signum() == 0) {
            throw new IllegalArgumentException("A consumption limit in watts must be greater than zero.");
        }
    }

    public boolean evaluate(BigDecimal currentValue, String conditionType, BigDecimal threshold) {
        if (currentValue == null || conditionType == null || threshold == null) {
            return false;
        }

        return switch (conditionType.toUpperCase()) {
            case "GREATER_THAN" -> currentValue.compareTo(threshold) > 0;
            case "LESS_THAN" -> currentValue.compareTo(threshold) < 0;
            case "EQUALS" -> currentValue.compareTo(threshold) == 0;
            default -> false;
        };
    }
}