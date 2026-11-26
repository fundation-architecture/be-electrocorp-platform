package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.application.facades.DeviceControlContextFacade;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.factories.EnergyReadingFactory;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyReadingRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.CurrentConsumptionService;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.EnergyCostCalculator;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.MonthlyCostProjectionService;
import com.electrocorp.electrocorpplatform.energymonitoring.infrastructure.cache.NoOpConsumptionSummaryCache;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.PeriodCostSummaryResource;
import com.electrocorp.electrocorpplatform.notifications.application.facades.ActiveAlertsOverview;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-04 (US-17): gasto acumulado del periodo con la tarifa de TariffProvider.
 */
class PeriodCostSummaryTest {

    private final EnergyReadingRepository repository = mock(EnergyReadingRepository.class);
    private final EnergyReadingFactory factory = new EnergyReadingFactory();
    private final EnergyMonitoringApplicationService queryService = new EnergyMonitoringApplicationService(
            repository,
            mock(DeviceControlContextFacade.class),
            userId -> ActiveAlertsOverview.empty(),
            new CurrentConsumptionService(),
            new NoOpConsumptionSummaryCache(),
            new EnergyCostCalculator(() -> new BigDecimal("0.75")),
            new MonthlyCostProjectionService()
    );

    @Test
    void accumulatesTheCostOfThePeriodByDevice() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 10);
        when(repository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(
                eq(7L), eq(from.atStartOfDay()), eq(to.plusDays(1).atStartOfDay().minusNanos(1))))
                .thenReturn(List.of(
                        reading(1L, "Terma", "10"),
                        reading(1L, "Terma", "6"),
                        reading(2L, "Refrigeradora", "4")
                ));

        PeriodCostSummaryResource summary = queryService.getPeriodCostSummary(7L, from, to);

        assertThat(summary.days()).isEqualTo(10);
        assertThat(summary.kilowattHours()).isEqualByComparingTo("20");
        assertThat(summary.accumulatedCost()).isEqualByComparingTo("15.00");
        assertThat(summary.averageDailyCost()).isEqualByComparingTo("1.50");
        assertThat(summary.currencySymbol()).isEqualTo("S/");
        assertThat(summary.devices()).extracting(PeriodCostSummaryResource.DeviceCost::deviceName)
                .containsExactly("Terma", "Refrigeradora");
        assertThat(summary.devices().get(0).accumulatedCost()).isEqualByComparingTo("12.00");
    }

    @Test
    void returnsZeroWhenThereAreNoReadings() {
        PeriodCostSummaryResource summary = queryService.getPeriodCostSummary(
                7L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(summary.accumulatedCost()).isEqualByComparingTo("0.00");
        assertThat(summary.devices()).isEmpty();
    }

    @Test
    void rejectsAPeriodThatEndsBeforeItStarts() {
        assertThatThrownBy(() -> queryService.getPeriodCostSummary(
                7L, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private EnergyReading reading(Long deviceId, String deviceName, String kilowattHours) {
        return factory.create(7L, deviceId, deviceName, new BigDecimal("500"), new BigDecimal(kilowattHours),
                BigDecimal.ZERO, 3600, LocalDateTime.of(2026, 11, 5, 10, 0));
    }
}
