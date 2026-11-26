package com.electrocorp.electrocorpplatform.energymonitoring.domain.model.queries;

import java.time.LocalDate;

/**
 * Consulta del gasto acumulado de un periodo (US-17). Si no se indican fechas, el periodo es
 * el mes en curso hasta el dia de hoy.
 */
public record GetPeriodCostSummaryQuery(
        Long userId,
        LocalDate from,
        LocalDate to
) {
}
