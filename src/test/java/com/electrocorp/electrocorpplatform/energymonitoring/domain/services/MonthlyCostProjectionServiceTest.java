package com.electrocorp.electrocorpplatform.energymonitoring.domain.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-32: proyeccion = costo acumulado del mes / dias transcurridos * dias del mes.
 */
class MonthlyCostProjectionServiceTest {

    private final MonthlyCostProjectionService projectionService = new MonthlyCostProjectionService();

    @Test
    @DisplayName("Inicio de mes: con S/ 2.00 el dia 1 de un mes de 30 dias se proyectan S/ 60.00")
    void projectsAtTheStartOfTheMonth() {
        BigDecimal projection = projectionService.project(new BigDecimal("2.00"), 1, 30);

        assertThat(projection).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("Mitad de mes: S/ 20.00 acumulados en 10 dias de un mes de 30 dias proyectan S/ 60.00")
    void projectsInTheMiddleOfTheMonth() {
        BigDecimal projection = projectionService.project(new BigDecimal("20.00"), 10, 30);

        assertThat(projection).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("Fin de mes: el ultimo dia la proyeccion es igual al costo acumulado")
    void projectsAtTheEndOfTheMonth() {
        BigDecimal projection = projectionService.project(new BigDecimal("93.17"), 31, 31);

        assertThat(projection).isEqualByComparingTo("93.17");
    }

    @Test
    @DisplayName("Sin lecturas: la proyeccion es S/ 0.00 sin dividir entre cero")
    void projectsZeroWhenThereAreNoReadings() {
        assertThat(projectionService.project(BigDecimal.ZERO, 10, 30)).isEqualByComparingTo("0.00");
        assertThat(projectionService.project(null, 10, 30)).isEqualByComparingTo("0.00");
        assertThat(projectionService.project(new BigDecimal("5.00"), 0, 30)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Usa la cantidad real de dias del mes de la fecha consultada")
    void usesTheRealLengthOfTheMonth() {
        BigDecimal projection = projectionService.project(new BigDecimal("14.50"), LocalDate.of(2028, 2, 10));

        assertThat(projection).isEqualByComparingTo("42.05");
    }

    @Test
    @DisplayName("Redondea la proyeccion a centimos")
    void roundsToCents() {
        BigDecimal projection = projectionService.project(new BigDecimal("10.00"), 3, 31);

        assertThat(projection).isEqualByComparingTo("103.33");
        assertThat(projection.scale()).isEqualTo(2);
    }
}
