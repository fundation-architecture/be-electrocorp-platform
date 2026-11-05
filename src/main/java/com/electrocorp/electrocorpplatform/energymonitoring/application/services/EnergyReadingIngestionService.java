package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.application.facades.DeviceControlContextFacade;
import com.electrocorp.electrocorpplatform.devicecontrol.application.facades.DeviceSnapshot;
import com.electrocorp.electrocorpplatform.energymonitoring.application.ports.ConsumptionSummaryCache;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.factories.EnergyReadingFactory;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.EnergyReadingStatus;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.commands.CreateEnergyReadingCommand;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.events.EnergyThresholdExceededEvent;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyReadingRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.EnergyCostCalculator;
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
    private final DeviceControlContextFacade deviceControlContextFacade;
    private final DomainEventPublisher domainEventPublisher;
    private final ConsumptionSummaryCache consumptionSummaryCache;
    private final EnergyCostCalculator energyCostCalculator;

    @Transactional
    public EnergyReading createReading(CreateEnergyReadingCommand command) {
        DeviceSnapshot device = deviceControlContextFacade.findDeviceOfUser(command.deviceId(), command.userId())
                .orElseThrow(() -> new IllegalArgumentException("Device not found."));
        int sampleSeconds = command.sampleSeconds() == null ? 0 : command.sampleSeconds();
        BigDecimal kilowattHours = energyCostCalculator.kilowattHoursOf(command.watts(), sampleSeconds);

        EnergyReading reading = energyReadingFactory.create(
                command.userId(),
                command.deviceId(),
                device.name(),
                command.watts(),
                kilowattHours,
                energyCostCalculator.preciseCostOf(kilowattHours),
                sampleSeconds,
                LocalDateTime.now()
        );
        EnergyReading savedReading = energyReadingRepository.save(reading);
        consumptionSummaryCache.evict(savedReading.getUserId());
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
