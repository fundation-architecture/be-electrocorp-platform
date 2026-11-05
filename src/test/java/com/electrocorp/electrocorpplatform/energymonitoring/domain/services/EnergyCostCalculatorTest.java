package com.electrocorp.electrocorpplatform.energymonitoring.domain.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyCostCalculatorTest {

    private final EnergyCostCalculator calculator = new EnergyCostCalculator(() -> new BigDecimal("0.75"));

    @Test
    void twoKilowattHoursAtSeventyFiveCentsCostOneSolFifty() {
        assertThat(calculator.costOf(new BigDecimal("2"))).isEqualByComparingTo("1.50");
    }

    @Test
    void costPerHourUsesTheTariffFromTheProvider() {
        assertThat(calculator.costPerHour(new BigDecimal("2000"))).isEqualByComparingTo("1.50");
    }

    @Test
    void kilowattHoursOfAConstantPowerDuringAnHour() {
        assertThat(calculator.kilowattHoursOf(new BigDecimal("2000"), 3600)).isEqualByComparingTo("2");
    }

    @Test
    void zeroConsumptionCostsNothing() {
        assertThat(calculator.costOf(BigDecimal.ZERO)).isEqualByComparingTo("0.00");
        assertThat(calculator.costPerHour(null)).isEqualByComparingTo("0.00");
    }

    @Test
    void currencyIsExpressedInSoles() {
        assertThat(EnergyCostCalculator.CURRENCY).isEqualTo("PEN");
        assertThat(EnergyCostCalculator.CURRENCY_SYMBOL).isEqualTo("S/");
    }
}
