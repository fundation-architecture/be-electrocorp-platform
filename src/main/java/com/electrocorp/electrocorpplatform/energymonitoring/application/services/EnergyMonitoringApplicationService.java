package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Device;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.DeviceStatus;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.DeviceRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.commands.CreateEnergyReadingCommand;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.factories.EnergyReadingFactory;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.EnergyReadingStatus;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.events.EnergyThresholdExceededEvent;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyReadingRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.EnergyDashboardSummaryResource;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.Alert;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.AlertRepository;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnergyMonitoringApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnergyMonitoringApplicationService.class);

    private static final BigDecimal TARIFF_PER_KWH = BigDecimal.valueOf(0.75);
    private final EnergyReadingFactory energyReadingFactory = new EnergyReadingFactory();

    private final EnergyReadingRepository energyReadingRepository;
    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;
    private final EnergyReadingRecorderService energyReadingRecorderService;
    private final EnergySamplingSettingsService energySamplingSettingsService;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public List<EnergyReading> getReadings(Long userId) {
        recordDueActiveDeviceIntervals(userId);
        return energyReadingRepository.findByUserIdOrderByRecordedAtDesc(userId);
    }

    @Transactional
    public List<EnergyReading> getReadings(Long userId, LocalDateTime start, LocalDateTime end) {
        recordDueActiveDeviceIntervals(userId);
        return energyReadingRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(userId, start, end);
    }

    @Transactional
    public EnergyDashboardSummaryResource getDashboardSummary(Long userId) {
        List<Device> devices = recordDueActiveDeviceIntervals(userId);
        List<Device> activeDevices = devices.stream()
                .filter(device -> device.getStatus() == DeviceStatus.ON)
                .toList();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);
        List<EnergyReading> todayReadings = energyReadingRepository
                .findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(userId, startOfDay, endOfDay);

        BigDecimal currentWatts = activeDevices.stream()
                .map(Device::getPowerWatts)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal todayKilowattHours = todayReadings.stream()
                .map(this::safeKilowattHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);

        BigDecimal todayEstimatedCost = todayReadings.stream()
                .map(this::safeEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal costPerHour = calculateCostPerHour(currentWatts);
        BigDecimal projectedMonthlyCost = projectMonthlyCost(todayEstimatedCost);
        BigDecimal peakWatts = todayReadings.stream()
                .map(EnergyReading::getWatts)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal averageWatts = calculateAverageWatts(todayReadings);

        int highReadings = (int) todayReadings.stream()
                .filter(reading -> reading.getStatus() == EnergyReadingStatus.HIGH)
                .count();
        int normalReadings = Math.max(todayReadings.size() - highReadings, 0);
        int efficiencyScore = calculateEfficiencyScore(todayReadings.size(), highReadings);
        List<Alert> activeAlerts = alertRepository.findByUserIdAndActiveTrue(userId);
        int criticalAlerts = (int) activeAlerts.stream()
                .filter(Alert::isCritical)
                .count();
        Alert latestAlert = activeAlerts.stream()
                .max(Comparator.comparing(this::alertLastTriggeredAt))
                .orElse(null);

        return new EnergyDashboardSummaryResource(
                currentWatts,
                todayKilowattHours,
                todayEstimatedCost,
                projectedMonthlyCost,
                costPerHour,
                peakWatts,
                averageWatts,
                activeDevices.size(),
                devices.size(),
                normalReadings,
                highReadings,
                efficiencyScore,
                resolveOperationalStatus(efficiencyScore),
                resolveRecommendation(activeDevices, todayEstimatedCost, projectedMonthlyCost),
                activeAlerts.size(),
                criticalAlerts,
                latestAlert == null ? null : latestAlert.getLevel(),
                latestAlert == null ? null : latestAlert.getTitle(),
                buildTrend(todayReadings),
                buildTopDevices(todayReadings, devices),
                buildRoomConsumption(todayReadings, activeDevices, devices),
                buildActiveDeviceDetails(activeDevices)
        );
    }

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

    private BigDecimal calculateAverageWatts(List<EnergyReading> readings) {
        if (readings.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = readings.stream()
                .map(EnergyReading::getWatts)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(readings.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCostPerHour(BigDecimal watts) {
        return watts
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(TARIFF_PER_KWH)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal projectMonthlyCost(BigDecimal todayEstimatedCost) {
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        int daysInMonth = YearMonth.now().lengthOfMonth();

        if (dayOfMonth <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return todayEstimatedCost
                .multiply(BigDecimal.valueOf(daysInMonth))
                .divide(BigDecimal.valueOf(dayOfMonth), 2, RoundingMode.HALF_UP);
    }

    private int calculateEfficiencyScore(int totalReadings, int highReadings) {
        if (totalReadings == 0) {
            return 100;
        }

        double highRatio = (double) highReadings / totalReadings;
        return Math.max(0, (int) Math.round(100 - highRatio * 100));
    }

    private String resolveOperationalStatus(int efficiencyScore) {
        if (efficiencyScore >= 90) return "Optimo";
        if (efficiencyScore >= 75) return "Estable";
        if (efficiencyScore >= 55) return "Atencion";
        return "Critico";
    }

    private String resolveRecommendation(
            List<Device> activeDevices,
            BigDecimal todayEstimatedCost,
            BigDecimal projectedMonthlyCost
    ) {
        if (activeDevices.isEmpty()) {
            return "No hay dispositivos encendidos. El consumo actual esta en reposo.";
        }

        Device highestActiveDevice = activeDevices.stream()
                .filter(device -> device.getPowerWatts() != null)
                .max(Comparator.comparing(Device::getPowerWatts))
                .orElse(null);

        if (highestActiveDevice == null) {
            return "Hay dispositivos activos sin potencia registrada. Completa sus watts para estimar costos.";
        }

        return "El mayor consumo activo es " + highestActiveDevice.getName()
                + " con " + highestActiveDevice.getPowerWatts().setScale(2, RoundingMode.HALF_UP)
                + " W. Hoy vas en S/ " + todayEstimatedCost
                + " y la proyeccion mensual es S/ " + projectedMonthlyCost + ".";
    }

    private List<EnergyDashboardSummaryResource.TrendPoint> buildTrend(List<EnergyReading> readings) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        return readings.stream()
                .sorted(Comparator.comparing(EnergyReading::getRecordedAt))
                .skip(Math.max(readings.size() - 8, 0))
                .map(reading -> new EnergyDashboardSummaryResource.TrendPoint(
                        reading.getRecordedAt().format(formatter),
                        reading.getDeviceName(),
                        reading.getWatts(),
                        safeKilowattHours(reading),
                        safeEstimatedCost(reading),
                        reading.getSampleSeconds() != null ? reading.getSampleSeconds() : 0,
                        reading.getStatus() == EnergyReadingStatus.HIGH
                ))
                .toList();
    }

    private List<EnergyDashboardSummaryResource.DeviceConsumption> buildTopDevices(
            List<EnergyReading> readings,
            List<Device> devices
    ) {
        Map<Long, Device> deviceById = devices.stream()
                .collect(Collectors.toMap(Device::getId, Function.identity()));

        return readings.stream()
                .collect(Collectors.groupingBy(EnergyReading::getDeviceId))
                .entrySet()
                .stream()
                .map(entry -> toDeviceConsumption(entry.getKey(), entry.getValue(), deviceById.get(entry.getKey())))
                .sorted(Comparator.comparing(EnergyDashboardSummaryResource.DeviceConsumption::estimatedCost).reversed())
                .limit(6)
                .toList();
    }

    private EnergyDashboardSummaryResource.DeviceConsumption toDeviceConsumption(
            Long deviceId,
            List<EnergyReading> readings,
            Device device
    ) {
        BigDecimal watts = readings.stream()
                .map(EnergyReading::getWatts)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal kilowattHours = readings.stream()
                .map(this::safeKilowattHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal estimatedCost = readings.stream()
                .map(this::safeEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        int highReadings = (int) readings.stream()
                .filter(reading -> reading.getStatus() == EnergyReadingStatus.HIGH)
                .count();
        EnergyReading latest = readings.stream()
                .max(Comparator.comparing(EnergyReading::getRecordedAt))
                .orElseThrow();

        return new EnergyDashboardSummaryResource.DeviceConsumption(
                deviceId,
                latest.getDeviceName(),
                device != null ? device.getRoom() : "Sin ambiente",
                device != null ? device.getType() : "Sin tipo",
                watts,
                kilowattHours,
                estimatedCost,
                readings.size(),
                highReadings
        );
    }

    private List<EnergyDashboardSummaryResource.RoomConsumption> buildRoomConsumption(
            List<EnergyReading> readings,
            List<Device> activeDevices,
            List<Device> devices
    ) {
        Map<Long, Device> deviceById = devices.stream()
                .collect(Collectors.toMap(Device::getId, Function.identity()));
        Map<String, Long> activeCountByRoom = activeDevices.stream()
                .collect(Collectors.groupingBy(this::resolveRoom, Collectors.counting()));

        return readings.stream()
                .collect(Collectors.groupingBy(reading -> resolveRoom(deviceById.get(reading.getDeviceId()))))
                .entrySet()
                .stream()
                .map(entry -> {
                    BigDecimal watts = entry.getValue().stream()
                            .map(EnergyReading::getWatts)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal kilowattHours = entry.getValue().stream()
                            .map(this::safeKilowattHours)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(6, RoundingMode.HALF_UP);
                    BigDecimal estimatedCost = entry.getValue().stream()
                            .map(this::safeEstimatedCost)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);

                    return new EnergyDashboardSummaryResource.RoomConsumption(
                            entry.getKey(),
                            watts,
                            kilowattHours,
                            estimatedCost,
                            activeCountByRoom.getOrDefault(entry.getKey(), 0L).intValue()
                    );
                })
                .sorted(Comparator.comparing(EnergyDashboardSummaryResource.RoomConsumption::estimatedCost).reversed())
                .toList();
    }

    private List<EnergyDashboardSummaryResource.ActiveDevice> buildActiveDeviceDetails(List<Device> activeDevices) {
        return activeDevices.stream()
                .sorted(Comparator.comparing(Device::getPowerWatts, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(device -> new EnergyDashboardSummaryResource.ActiveDevice(
                        device.getId(),
                        device.getName(),
                        resolveRoom(device),
                        device.getType(),
                        device.getPowerWatts() != null ? device.getPowerWatts().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                        calculateCostPerHour(device.getPowerWatts() != null ? device.getPowerWatts() : BigDecimal.ZERO)
                ))
                .toList();
    }

    private String resolveRoom(Device device) {
        if (device == null || device.getRoom() == null || device.getRoom().isBlank()) {
            return "Sin ambiente";
        }

        return device.getRoom().trim();
    }

    private LocalDateTime alertLastTriggeredAt(Alert alert) {
        if (alert.getLastTriggeredAt() != null) {
            return alert.getLastTriggeredAt();
        }

        if (alert.getFirstDetectedAt() != null) {
            return alert.getFirstDetectedAt();
        }

        return LocalDateTime.MIN;
    }

    private BigDecimal safeKilowattHours(EnergyReading reading) {
        return reading.getKilowattHours() != null
                ? reading.getKilowattHours()
                : BigDecimal.ZERO;
    }

    private BigDecimal safeEstimatedCost(EnergyReading reading) {
        return reading.getEstimatedCost() != null
                ? reading.getEstimatedCost()
                : BigDecimal.ZERO;
    }

    private List<Device> recordDueActiveDeviceIntervals(Long userId) {
        List<Device> devices = deviceRepository.findByUserId(userId);
        int sampleSeconds = energySamplingSettingsService.getSampleSeconds();

        devices.stream()
                .filter(device -> device.getStatus() == DeviceStatus.ON)
                .forEach(device -> recordDueActiveDeviceInterval(device, sampleSeconds));

        return devices;
    }

    private void recordDueActiveDeviceInterval(Device device, int sampleSeconds) {
        try {
            energyReadingRecorderService.recordActiveDeviceIntervalIfDue(device, sampleSeconds);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not record due energy reading for device {}", device.getId(), exception);
        }
    }
}
