package com.electrocorp.electrocorpplatform.energymonitoring.domain.services;

import java.math.BigDecimal;

/**
 * Puerto de dominio que entrega la tarifa electrica vigente en soles por kWh (QA-06).
 * Hoy la implementa un adaptador configurable; mas adelante podra leer el pliego tarifario
 * de OSINERGMIN sin cambiar el calculo de costos.
 */
public interface TariffProvider {

    BigDecimal tariffPerKilowattHour();
}
