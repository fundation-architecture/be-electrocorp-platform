package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.DeviceStatus;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Device;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.DeviceRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyReadingRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.CurrentConsumptionService;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.AlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final EnergyMonitoringApplicationService queryService = new EnergyMonitoringApplicationService(
            energyReadingRepository,
            deviceRepository,
            alertRepository,
            new CurrentConsumptionService()
    );

    @Test
    void readingQueriesAndDashboardSummaryDoNotInsertReadings() {
        when(deviceRepository.findByUserId(7L)).thenReturn(List.of(device(1L, 7L, DeviceStatus.ON)));
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
                deviceRepository,
                recorderService,
                settingsService
        );
        Device activeDevice = device(1L, 7L, DeviceStatus.ON);
        Device inactiveDevice = device(2L, 7L, DeviceStatus.OFF);
        when(deviceRepository.findAll()).thenReturn(List.of(activeDevice, inactiveDevice));

        scheduler.recordDueActiveDeviceIntervals();

        verify(recorderService).recordActiveDeviceIntervalIfDue(activeDevice, settingsService.getSampleSeconds());
        verify(recorderService, never()).recordActiveDeviceIntervalIfDue(org.mockito.ArgumentMatchers.eq(inactiveDevice), anyInt());
    }

    private long countSaveInvocations() {
        return mockingDetails(energyReadingRepository).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("save"))
                .count();
    }

    private Device device(Long id, Long userId, DeviceStatus status) {
        Device device = new Device();
        ReflectionTestUtils.setField(device, "id", id);
        device.setUserId(userId);
        device.setName("Dispositivo " + id);
        device.setType("Aire acondicionado");
        device.setRoom("Sala");
        device.setPowerWatts(BigDecimal.valueOf(900));
        device.setStatus(status);
        return device;
    }
}
