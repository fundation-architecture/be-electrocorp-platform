package com.electrocorp.electrocorpplatform.notifications.domain.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-13 (US-19): el limite de consumo se valida antes de guardarlo.
 */
class AlertRuleValidationTest {

    private final AlertRulePolicyService policy = new AlertRulePolicyService();

    @Test
    void acceptsAValidPowerLimitInWatts() {
        assertThatCode(() -> policy.validateRule("Limite", "WATTS", "GREATER_THAN", new BigDecimal("1000")))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validateRule("Limite", "WATTS", "GREATER_OR_EQUAL_THAN", new BigDecimal("1000")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsANegativeOrZeroLimitInWatts() {
        assertThatThrownBy(() -> policy.validateRule("Limite", "WATTS", "GREATER_THAN", new BigDecimal("-5")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validateRule("Limite", "WATTS", "GREATER_THAN", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnUnknownMetricOrCondition() {
        assertThatThrownBy(() -> policy.validateRule("Limite", "VOLTS", "GREATER_THAN", BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validateRule("Limite", "WATTS", "BETWEEN", BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
