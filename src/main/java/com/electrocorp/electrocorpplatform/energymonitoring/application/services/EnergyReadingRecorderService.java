package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.application.facades.DeviceSnapshot;
import com.electrocorp.electrocorpplatform.energymonitoring.application.ports.ConsumptionSummaryCache;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.factories.EnergyReadingFactory;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.EnergyDeviceUsageState;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyDeviceUsageStateRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyReadingRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.EnergyConsumptionSimulatorService;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.EnergyCostCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EnergyReadingRecorderService {

    private static final int MAX_SAMPLE_SECONDS = 3600;
    private final EnergyDeviceUsageStateRepository usageStateRepository;
    private final EnergyReadingRepository energyReadingRepository;
    private final EnergyConsumptionSimulatorService simulatorService;
    private final EnergyReadingFactory energyReadingFactory;
    private final EnergyThresholdMonitor energyThresholdMonitor;
    private final ConsumptionSummaryCache consumptionSummaryCache;
    private final EnergyCostCalculator energyCostCalculator;

    @Transactional
    public void recordStatusTransition(DeviceSnapshot device, boolean wasActive) {
        boolean isActive = device.active();

        if (wasActive == isActive) {
            return;
        }

        if (isActive) {
            startUsage(device, LocalDateTime.now());
            return;
        }

        if (wasActive) {
            closeUsage(device, LocalDateTime.now());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EnergyReading recordActiveDeviceInterval(DeviceSnapshot device) {
        if (!device.active()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        EnergyDeviceUsageState state = usageStateRepository.findByDeviceId(device.id())
                .orElseGet(() -> startUsage(device, now));

        return recordUsageSince(device, state, now, true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EnergyReading recordActiveDeviceIntervalIfDue(DeviceSnapshot device, int minimumSeconds) {
        if (!device.active()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        int normalizedMinimumSeconds = Math.max(1, Math.min(MAX_SAMPLE_SECONDS, minimumSeconds));
        EnergyDeviceUsageState state = usageStateRepository.findByDeviceId(device.id())
                .orElseGet(() -> startUsage(device, now.minusSeconds(normalizedMinimumSeconds)));
        LocalDateTime lastRecordedAt = state.getLastRecordedAt() != null
                ? state.getLastRecordedAt()
                : state.getStartedAt();

        if (lastRecordedAt == null) {
            return null;
        }

        long totalUsageSeconds = Math.max(0, Duration.between(lastRecordedAt, now).getSeconds());

        if (totalUsageSeconds < normalizedMinimumSeconds) {
            return null;
        }

        return recordUsageSince(device, state, now, true);
    }

    @Transactional
    public EnergyReading closeUsage(DeviceSnapshot device) {
        return closeUsage(device, LocalDateTime.now());
    }

    private EnergyDeviceUsageState startUsage(DeviceSnapshot device, LocalDateTime now) {
        return usageStateRepository.findByDeviceId(device.id())
                .orElseGet(() -> {
                    EnergyDeviceUsageState state = new EnergyDeviceUsageState();
                    state.setUserId(device.userId());
                    state.setDeviceId(device.id());
                    state.setStartedAt(now);
                    state.setLastRecordedAt(now);
                    return usageStateRepository.save(state);
                });
    }

    private EnergyReading closeUsage(DeviceSnapshot device, LocalDateTime now) {
        return usageStateRepository.findByDeviceId(device.id())
                .map(state -> {
                    EnergyReading reading = recordUsageSince(device, state, now, false);
                    usageStateRepository.delete(state);
                    return reading;
                })
                .orElse(null);
    }

    private EnergyReading recordUsageSince(
            DeviceSnapshot device,
            EnergyDeviceUsageState state,
            LocalDateTime now,
            boolean keepStateOpen
    ) {
        LocalDateTime lastRecordedAt = state.getLastRecordedAt() != null
                ? state.getLastRecordedAt()
                : state.getStartedAt();

        if (lastRecordedAt == null) {
            state.setStartedAt(now);
            state.setLastRecordedAt(now);
            usageStateRepository.save(state);
            return null;
        }

        long totalUsageSeconds = Math.max(0, Duration.between(lastRecordedAt, now).getSeconds());

        if (totalUsageSeconds <= 0) {
            return null;
        }

        BigDecimal watts = calculateWatts(device);
        EnergyReading savedReading = null;
        LocalDateTime cursor = lastRecordedAt;
        long remainingSeconds = totalUsageSeconds;

        while (remainingSeconds > 0) {
            int sampleSeconds = (int) Math.min(MAX_SAMPLE_SECONDS, remainingSeconds);
            cursor = cursor.plusSeconds(sampleSeconds);

            BigDecimal kilowattHours = energyCostCalculator.kilowattHoursOf(watts, sampleSeconds);

            EnergyReading reading = energyReadingFactory.create(
                    device.userId(),
                    device.id(),
                    device.name(),
                    watts,
                    kilowattHours,
                    energyCostCalculator.preciseCostOf(kilowattHours),
                    sampleSeconds,
                    cursor
            );

            savedReading = energyReadingRepository.save(reading);
            energyThresholdMonitor.publishIfExceeded(savedReading);
            remainingSeconds -= sampleSeconds;
        }

        if (savedReading != null) {
            consumptionSummaryCache.evict(device.userId());
        }

        if (keepStateOpen) {
            state.setLastRecordedAt(now);
            usageStateRepository.save(state);
        }

        return savedReading;
    }

    private BigDecimal calculateWatts(DeviceSnapshot device) {
        return simulatorService.simulateWatts(device.powerWatts());
    }
}
