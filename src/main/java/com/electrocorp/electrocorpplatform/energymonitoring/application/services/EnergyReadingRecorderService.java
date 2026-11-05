package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.application.facades.DeviceSnapshot;
import com.electrocorp.electrocorpplatform.energymonitoring.application.ports.ConsumptionSummaryCache;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.factories.EnergyReadingFactory;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.EnergyDeviceUsageState;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.EnergyReadingStatus;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.events.EnergyThresholdExceededEvent;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyDeviceUsageStateRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyReadingRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.EnergyConsumptionSimulatorService;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.TariffProvider;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EnergyReadingRecorderService {

    private static final int MAX_SAMPLE_SECONDS = 3600;
    private final EnergyReadingFactory energyReadingFactory = new EnergyReadingFactory();

    private final EnergyDeviceUsageStateRepository usageStateRepository;
    private final EnergyReadingRepository energyReadingRepository;
    private final EnergyConsumptionSimulatorService simulatorService;
    private final DomainEventPublisher domainEventPublisher;
    private final ConsumptionSummaryCache consumptionSummaryCache;
    private final TariffProvider tariffProvider;

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

            BigDecimal kilowattHours = calculateKilowattHours(watts, sampleSeconds);

            EnergyReading reading = energyReadingFactory.create(
                    device.userId(),
                    device.id(),
                    device.name(),
                    watts,
                    kilowattHours,
                    calculateEstimatedCost(kilowattHours),
                    sampleSeconds,
                    cursor
            );

            savedReading = energyReadingRepository.save(reading);
            publishThresholdExceededIfNeeded(savedReading);
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

    private BigDecimal calculateKilowattHours(BigDecimal watts, int sampleSeconds) {
        if (sampleSeconds <= 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }

        return watts
                .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(sampleSeconds))
                .divide(BigDecimal.valueOf(3600), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEstimatedCost(BigDecimal kilowattHours) {
        return kilowattHours
                .multiply(tariffProvider.tariffPerKilowattHour())
                .setScale(6, RoundingMode.HALF_UP);
    }

    private void publishThresholdExceededIfNeeded(EnergyReading reading) {
        if (reading == null || reading.getWatts() == null || reading.getStatus() != EnergyReadingStatus.HIGH) {
            return;
        }

        domainEventPublisher.publish(new EnergyThresholdExceededEvent(
                reading.getUserId(),
                reading.getDeviceId(),
                reading.getWatts()
        ));
    }

}
