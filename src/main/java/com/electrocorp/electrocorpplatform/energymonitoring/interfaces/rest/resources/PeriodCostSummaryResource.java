package com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Gasto electrico acumulado del usuario en un periodo (US-17).")
public record PeriodCostSummaryResource(
        @Schema(description = "Primer dia del periodo (incluido).", example = "2026-11-01")
        LocalDate from,
        @Schema(description = "Ultimo dia del periodo (incluido).", example = "2026-11-26")
        LocalDate to,
        @Schema(description = "Dias del periodo.", example = "26")
        Integer days,
        @Schema(description = "Energia consumida en el periodo en kWh.", example = "42.500000")
        BigDecimal kilowattHours,
        @Schema(description = "Costo acumulado del periodo en soles (S/).", example = "31.88")
        BigDecimal accumulatedCost,
        @Schema(description = "Costo promedio por dia en soles (S/).", example = "1.23")
        BigDecimal averageDailyCost,
        @Schema(description = "Cantidad de lecturas registradas en el periodo.", example = "120")
        Integer readings,
        @Schema(description = "Codigo ISO 4217 de la moneda.", example = "PEN")
        String currency,
        @Schema(description = "Simbolo de la moneda.", example = "S/")
        String currencySymbol,
        @Schema(description = "Gasto acumulado por dispositivo, de mayor a menor costo.")
        List<DeviceCost> devices
) {
    public record DeviceCost(
            Long deviceId,
            String deviceName,
            BigDecimal kilowattHours,
            BigDecimal accumulatedCost
    ) {
    }
}
