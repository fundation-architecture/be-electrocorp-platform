package com.electrocorp.electrocorpplatform.acceptance;

import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.MonthlyCostProjectionService;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pasos de la prueba de aceptacion de US-18 (Proyeccion del gasto mensual).
 */
public class MonthlyProjectionSteps {

    @Autowired
    private SprintTwoApiClient api;

    @Autowired
    private MonthlyCostProjectionService projectionService;

    private String token;
    private HttpResponse<String> summaryResponse;
    private BigDecimal accumulatedCost;
    private int daysElapsed;
    private int daysInMonth;
    private BigDecimal projection;

    @Dado("que el sistema dispone de registros suficientes del consumo energético del usuario")
    public void thereAreEnoughConsumptionRecords() {
        token = api.registerAndSignInNewUser();
        Long deviceId = api.createDevice(token, "Refrigeradora", 1000);
        HttpResponse<String> reading = api.registerReading(token, deviceId, "Refrigeradora", 1000, 3600);
        assertThat(reading.statusCode()).isEqualTo(200);
    }

    @Cuando("este consulta la proyección mensual")
    public void theUserChecksTheMonthlyProjection() {
        summaryResponse = api.get(token, "/api/v1/energy-readings/dashboard-summary");
    }

    @Entonces("el sistema estima y muestra el gasto esperado al finalizar el periodo de facturación")
    public void showsTheExpectedMonthlyCost() {
        assertThat(summaryResponse.statusCode()).isEqualTo(200);
        JsonNode summary = api.json(summaryResponse);
        BigDecimal monthToDateCost = summary.get("monthToDateCost").decimalValue();
        BigDecimal projectedMonthlyCost = summary.get("projectedMonthlyCost").decimalValue();

        assertThat(monthToDateCost).isPositive();
        assertThat(projectedMonthlyCost).isGreaterThanOrEqualTo(monthToDateCost);
        assertThat(projectedMonthlyCost)
                .isEqualByComparingTo(projectionService.project(monthToDateCost, LocalDate.now()));
    }

    @Dado("^que en los primeros (\\d+) días de un mes de (\\d+) días el usuario acumuló S/ (\\d+\\.\\d{2}) de costo$")
    public void theUserAccumulatedCost(int daysElapsed, int daysInMonth, String accumulatedCost) {
        this.daysElapsed = daysElapsed;
        this.daysInMonth = daysInMonth;
        this.accumulatedCost = new BigDecimal(accumulatedCost);
    }

    @Cuando("consulta la proyección mensual")
    public void checksTheMonthlyProjection() {
        projection = projectionService.project(accumulatedCost, daysElapsed, daysInMonth);
    }

    @Entonces("^el sistema muestra una proyección de S/ (\\d+\\.\\d{2})$")
    public void showsAProjectionOf(String expectedProjection) {
        assertThat(projection).isEqualByComparingTo(new BigDecimal(expectedProjection));
    }
}
