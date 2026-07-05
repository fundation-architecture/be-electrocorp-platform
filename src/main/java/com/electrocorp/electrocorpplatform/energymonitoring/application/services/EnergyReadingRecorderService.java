package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Device;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.DeviceStatus;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.factories.EnergyReadingFactory;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.EnergyDeviceUsageState;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.EnergyReadingStatus;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.events.EnergyThresholdExceededEvent;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyDeviceUsageStateRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyReadingRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.EnergyConsumptionSimulatorService;
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

    private static final BigDecimal DEFAULT_TARIFF_PER_KWH = BigDecimal.valueOf(0.75);
    private static final int MAX_SAMPLE_SECONDS = 3600;
    private final EnergyReadingFactory energyReadingFactory = new EnergyReadingFactory();

    private final EnergyDeviceUsageStateRepository usageStateRepository;
    private final EnergyReadingRepository energyReadingRepository;
    private final EnergyConsumptionSimulatorService simulatorService;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public void recordStatusTransition(Device device, DeviceStatus previousStatus) {
        DeviceStatus currentStatus = device.getStatus();

        if (previousStatus == currentStatus) {
            return;
        }

        if (currentStatus == DeviceStatus.ON) {
            startUsage(device, LocalDateTime.now());
            return;
        }

        if (previousStatus == DeviceStatus.ON) {
            closeUsage(device, LocalDateTime.now());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EnergyReading recordActiveDeviceInterval(Device device) {
        if (device.getStatus() == null || device.getStatus() != DeviceStatus.ON) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        EnergyDeviceUsageState state = usageStateRepository.findByDeviceId(device.getId())
                .orElseGet(() -> startUsage(device, now));

        return recordUsageSince(device, state, now, true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EnergyReading recordActiveDeviceIntervalIfDue(Device device, int minimumSeconds) {
        if (device.getStatus() == null || device.getStatus() != DeviceStatus.ON) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        int normalizedMinimumSeconds = Math.max(1, Math.min(MAX_SAMPLE_SECONDS, minimumSeconds));
        EnergyDeviceUsageState state = usageStateRepository.findByDeviceId(device.getId())
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
    public EnergyReading closeUsage(Device device) {
        return closeUsage(device, LocalDateTime.now());
    }

    private EnergyDeviceUsageState startUsage(Device device, LocalDateTime now) {
        return usageStateRepository.findByDeviceId(device.getId())
                .orElseGet(() -> {
                    EnergyDeviceUsageState state = new EnergyDeviceUsageState();
                    state.setUserId(device.getUserId());
                    state.setDeviceId(device.getId());
                    state.setStartedAt(now);
                    state.setLastRecordedAt(now);
                    return usageStateRepository.save(state);
                });
    }

    private EnergyReading closeUsage(Device device, LocalDateTime now) {
        return usageStateRepository.findByDeviceId(device.getId())
                .map(state -> {
                    EnergyReading reading = recordUsageSince(device, state, now, false);
                    usageStateRepository.delete(state);
                    return reading;
                })
                .orElse(null);
    }

    private EnergyReading recordUsageSince(
            Device device,
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
                    device.getUserId(),
                    device.getId(),
                    device.getName(),
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

        if (keepStateOpen) {
            state.setLastRecordedAt(now);
            usageStateRepository.save(state);
        }

        return savedReading;
    }

    private BigDecimal calculateWatts(Device device) {
        return simulatorService.simulateWatts(device);
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
                .multiply(DEFAULT_TARIFF_PER_KWH)
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
