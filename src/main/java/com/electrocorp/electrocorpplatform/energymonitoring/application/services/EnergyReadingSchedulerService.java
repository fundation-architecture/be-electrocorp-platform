package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Device;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.DeviceStatus;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Planificador de ingesta (Telemetry Ingestion). Es el unico punto que registra los
 * intervalos pendientes de los dispositivos encendidos; las consultas del panel ya no escriben datos.
 */
@Service
@RequiredArgsConstructor
public class EnergyReadingSchedulerService implements SchedulingConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnergyReadingSchedulerService.class);

    private final DeviceRepository deviceRepository;
    private final EnergyReadingRecorderService recorderService;
    private final EnergySamplingSettingsService settingsService;

    public void recordDueActiveDeviceIntervals() {
        int sampleSeconds = settingsService.getSampleSeconds();
        List<Device> activeDevices = deviceRepository.findAll()
                .stream()
                .filter(device -> device.getStatus() == DeviceStatus.ON)
                .toList();

        activeDevices.forEach(device -> recordDueActiveDeviceInterval(device, sampleSeconds));
    }

    private void recordDueActiveDeviceInterval(Device device, int sampleSeconds) {
        try {
            recorderService.recordActiveDeviceIntervalIfDue(device, sampleSeconds);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not record scheduled energy reading for device {}", device.getId(), exception);
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                this::recordDueActiveDeviceIntervals,
                context -> {
                    Instant lastCompletion = context.lastCompletion();
                    Instant nextRunBase = lastCompletion != null ? lastCompletion : Instant.now();
                    return nextRunBase.plusSeconds(settingsService.getSampleSeconds());
                }
        );
    }
}
