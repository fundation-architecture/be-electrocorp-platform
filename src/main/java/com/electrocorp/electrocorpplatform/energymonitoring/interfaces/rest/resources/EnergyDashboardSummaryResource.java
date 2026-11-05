package com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Resumen del panel de consumo energetico del usuario (US-13, US-16 y US-18).")
public record EnergyDashboardSummaryResource(
        @Schema(description = "Potencia actual en watts: ultima lectura de cada dispositivo encendido.", example = "1150.00")
        BigDecimal currentWatts,
        @Schema(description = "Energia consumida hoy en kWh.", example = "2.000000")
        BigDecimal todayKilowattHours,
        @Schema(description = "Costo estimado de hoy en soles (S/), calculado con la tarifa de TariffProvider.", example = "1.50")
        BigDecimal todayEstimatedCost,
        @Schema(description = "Costo acumulado del mes en curso en soles (S/).", example = "20.00")
        BigDecimal monthToDateCost,
        @Schema(description = "Proyeccion del gasto mensual en soles (S/): costo acumulado del mes / dias transcurridos x dias del mes.", example = "60.00")
        BigDecimal projectedMonthlyCost,
        @Schema(description = "Costo por hora en soles (S/) si la potencia actual se mantiene.", example = "0.86")
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
        @Schema(description = "Codigo ISO 4217 de la moneda de los costos.", example = "PEN")
        String currency,
        @Schema(description = "Simbolo de la moneda de los costos.", example = "S/")
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
