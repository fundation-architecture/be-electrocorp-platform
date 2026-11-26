package com.electrocorp.electrocorpplatform.notifications.application.internal.facades;

import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.AlertRule;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.RuleScopeType;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.AlertRepository;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.AlertRuleRepository;
import com.electrocorp.electrocorpplatform.notifications.domain.services.AlertRulePolicyService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-06 (US-19): el limite configurado por el usuario se valida al guardarlo y se expone a
 * Energy Monitoring mediante NotificationsContextFacade.
 */
class ConsumptionLimitFacadeTest {

    private final AlertRuleRepository ruleRepository = mock(AlertRuleRepository.class);
    private final NotificationsContextFacadeImpl facade =
            new NotificationsContextFacadeImpl(mock(AlertRepository.class), ruleRepository);

    @Test
    void returnsTheLowestWattsLimitThatAppliesToTheDevice() {
        when(ruleRepository.findByUserIdAndEnabledTrue(7L)).thenReturn(List.of(
                rule("WATTS", "GREATER_THAN", "2000", RuleScopeType.DEVICE, "33"),
                rule("WATTS", "GREATER_THAN", "1200", RuleScopeType.GENERAL, null),
                rule("WATTS", "GREATER_THAN", "500", RuleScopeType.DEVICE, "99"),
                rule("WATTS", "LESS_THAN", "100", RuleScopeType.DEVICE, "33"),
                rule("DAILY_CONSUMPTION", "GREATER_THAN", "10", RuleScopeType.DEVICE, "33")
        ));

        assertThat(facade.findConsumptionLimitWatts(7L, 33L)).contains(new BigDecimal("1200"));
    }

    @Test
    void returnsEmptyWhenTheUserHasNoLimit() {
        when(ruleRepository.findByUserIdAndEnabledTrue(7L)).thenReturn(List.of());

        assertThat(facade.findConsumptionLimitWatts(7L, 33L)).isEmpty();
    }

    @Test
    void rejectsAWattsLimitOfZeroOrAnUnknownMetric() {
        AlertRulePolicyService policy = new AlertRulePolicyService();

        assertThatThrownBy(() -> policy.validateRule("Limite", "WATTS", "GREATER_THAN", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validateRule("Limite", "VOLTS", "GREATER_THAN", BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private AlertRule rule(String metric, String condition, String threshold, RuleScopeType scopeType, String scopeId) {
        AlertRule rule = new AlertRule();
        rule.setUserId(7L);
        rule.setName("Limite");
        rule.setMetric(metric);
        rule.setConditionType(condition);
        rule.setThreshold(new BigDecimal(threshold));
        rule.setScopeType(scopeType);
        rule.setScopeId(scopeId);
        rule.setEnabled(true);
        return rule;
    }
}
