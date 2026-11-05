package com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.List;

public record EnergyDashboardSummaryResource(
        BigDecimal currentWatts,
        BigDecimal todayKilowattHours,
        BigDecimal todayEstimatedCost,
        BigDecimal projectedMonthlyCost,
        BigDecimal costPerHour,
        BigDecimal peakWatts,
        BigDecimal averageWatts,
        Integer activeDevices,
        Integer monitoredDevices,
        Integer normalReadings,
        Integer highReadings,
        Integer efficiencyScore,
        String operationalStatus,
        String recommendation,
        Integer activeAlerts,
        Integer criticalAlerts,
        String latestAlertLevel,
        String latestAlertTitle,
        List<TrendPoint> trend,
        List<DeviceConsumption> topDevices,
        List<RoomConsumption> rooms,
        List<ActiveDevice> activeDeviceDetails,
        String currency,
        String currencySymbol
) {
    public record TrendPoint(
            String label,
            String deviceName,
            BigDecimal watts,
            BigDecimal kilowattHours,
            BigDecimal estimatedCost,
            Integer sampleSeconds,
            Boolean high
    ) {
    }

    public record DeviceConsumption(
            Long deviceId,
            String name,
            String room,
            String type,
            BigDecimal watts,
            BigDecimal kilowattHours,
            BigDecimal estimatedCost,
            Integer readings,
            Integer highReadings
    ) {
    }

    public record RoomConsumption(
            String room,
            BigDecimal watts,
            BigDecimal kilowattHours,
            BigDecimal estimatedCost,
            Integer activeDevices
    ) {
    }

    public record ActiveDevice(
            Long deviceId,
            String name,
            String room,
            String type,
            BigDecimal watts,
            BigDecimal costPerHour
    ) {
    }
}
