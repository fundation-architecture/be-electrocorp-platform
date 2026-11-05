package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Device;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.DeviceRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.factories.EnergyReadingFactory;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.EnergyReadingStatus;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.commands.CreateEnergyReadingCommand;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.events.EnergyThresholdExceededEvent;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyReadingRepository;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lado de escritura (Telemetry Ingestion) de Energy Monitoring: registra las lecturas
 * enviadas por los dispositivos. Las consultas viven en {@link EnergyMonitoringApplicationService}.
 */
@Service
@RequiredArgsConstructor
public class EnergyReadingIngestionService {

    private final EnergyReadingFactory energyReadingFactory = new EnergyReadingFactory();

    private final EnergyReadingRepository energyReadingRepository;
    private final DeviceRepository deviceRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public EnergyReading createReading(CreateEnergyReadingCommand command) {
        Device device = deviceRepository.findByIdAndUserId(command.deviceId(), command.userId())
                .orElseThrow(() -> new IllegalArgumentException("Device not found."));

        EnergyReading reading = energyReadingFactory.create(
                command.userId(),
                command.deviceId(),
                device.getName(),
                command.watts(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                LocalDateTime.now()
        );
        EnergyReading savedReading = energyReadingRepository.save(reading);
        publishThresholdExceededIfNeeded(savedReading);
        return savedReading;
    }

    private void publishThresholdExceededIfNeeded(EnergyReading reading) {
        if (reading != null && reading.getStatus() == EnergyReadingStatus.HIGH) {
            domainEventPublisher.publish(new EnergyThresholdExceededEvent(
                    reading.getUserId(),
                    reading.getDeviceId(),
                    reading.getWatts()
            ));
        }
    }
}
