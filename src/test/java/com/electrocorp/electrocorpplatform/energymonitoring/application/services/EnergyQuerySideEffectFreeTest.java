package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.application.facades.DeviceControlContextFacade;
import com.electrocorp.electrocorpplatform.devicecontrol.application.facades.DeviceSnapshot;
import com.electrocorp.electrocorpplatform.energymonitoring.infrastructure.cache.NoOpConsumptionSummaryCache;
import com.electrocorp.electrocorpplatform.energymonitoring.infrastructure.tariff.ConfigurableTariffProvider;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyReadingRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.CurrentConsumptionService;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.EnergyCostCalculator;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.MonthlyCostProjectionService;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.AlertRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-24 (CQRS): las consultas de Energy Monitoring no escriben en la base de datos y la
 * ingesta de intervalos pendientes se dispara solo desde el planificador.
 */
class EnergyQuerySideEffectFreeTest {

    private final EnergyReadingRepository energyReadingRepository = mock(EnergyReadingRepository.class);
    private final DeviceControlContextFacade deviceControlContextFacade = mock(DeviceControlContextFacade.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final EnergyMonitoringApplicationService queryService = new EnergyMonitoringApplicationService(
            energyReadingRepository,
            deviceControlContextFacade,
            alertRepository,
            new CurrentConsumptionService(),
            new NoOpConsumptionSummaryCache(),
            new EnergyCostCalculator(new ConfigurableTariffProvider(new BigDecimal("0.75"))),
            new MonthlyCostProjectionService()
    );

    @Test
    void readingQueriesAndDashboardSummaryDoNotInsertReadings() {
        when(deviceControlContextFacade.findDevicesByUserId(7L)).thenReturn(List.of(device(1L, 7L, true)));
        long insertsBefore = countSaveInvocations();

        queryService.getReadings(7L);
        queryService.getReadings(7L, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        queryService.getDashboardSummary(7L);

        long insertsAfter = countSaveInvocations();
        assertThat(insertsAfter).isEqualTo(insertsBefore);
        verify(energyReadingRepository, never()).save(any());
    }

    @Test
    void schedulerRecordsDueIntervalsOnlyForActiveDevices() {
        EnergyReadingRecorderService recorderService = mock(EnergyReadingRecorderService.class);
        EnergySamplingSettingsService settingsService = new EnergySamplingSettingsService();
        EnergyReadingSchedulerService scheduler = new EnergyReadingSchedulerService(
                deviceControlContextFacade,
                recorderService,
                settingsService
        );
        DeviceSnapshot activeDevice = device(1L, 7L, true);
        DeviceSnapshot inactiveDevice = device(2L, 7L, false);
        when(deviceControlContextFacade.findActiveDevices()).thenReturn(List.of(activeDevice));

        scheduler.recordDueActiveDeviceIntervals();

        verify(recorderService).recordActiveDeviceIntervalIfDue(activeDevice, settingsService.getSampleSeconds());
        verify(recorderService, never()).recordActiveDeviceIntervalIfDue(org.mockito.ArgumentMatchers.eq(inactiveDevice), anyInt());
    }

    private long countSaveInvocations() {
        return mockingDetails(energyReadingRepository).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("save"))
                .count();
    }

    private DeviceSnapshot device(Long id, Long userId, boolean active) {
        return new DeviceSnapshot(id, userId, "Dispositivo " + id, "Sala", "Aire acondicionado", BigDecimal.valueOf(900), active);
    }
}
