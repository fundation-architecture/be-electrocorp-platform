package com.electrocorp.electrocorpplatform.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pasos de la prueba de aceptacion de US-17 (Consulta del gasto acumulado del periodo).
 * Con la tarifa de S/ 0.75 por kWh: 2 kWh = S/ 1.50 y 3 kWh = S/ 2.25.
 */
public class PeriodCostSteps {

    private static final int ONE_HOUR = 3600;

    @Autowired
    private SprintTwoApiClient api;

    private String token;
    private String otherUserToken;
    private HttpResponse<String> summaryResponse;
    private HttpResponse<String> periodResponse;

    @Dado("que el usuario tiene lecturas del mes actual con costos de S\\/ {double} y S\\/ {double}")
    public void userHasReadingsWithCosts(double firstCost, double secondCost) {
        token = api.registerAndSignInNewUser();
        registerReadingWithCost(token, firstCost);
        registerReadingWithCost(token, secondCost);
    }

    @Dado("que el usuario no tiene lecturas registradas durante el mes actual")
    public void userHasNoReadings() {
        token = api.registerAndSignInNewUser();
    }

    @Y("otro usuario acumula S\\/ {double} durante el mismo mes")
    public void anotherUserAccumulates(double cost) {
        otherUserToken = api.registerAndSignInNewUser();
        registerReadingWithCost(otherUserToken, cost);
    }

    @Cuando("consulta el gasto acumulado del mes actual")
    public void checksTheMonthToDateCost() {
        summaryResponse = api.get(token, "/api/v1/energy-readings/dashboard-summary");
        periodResponse = api.get(token, "/api/v1/energy-readings/period-summary");
    }

    @Cuando("consulta el gasto acumulado del mes anterior")
    public void checksThePreviousMonth() {
        LocalDate firstDayOfPreviousMonth = LocalDate.now().withDayOfMonth(1).minusMonths(1);
        LocalDate lastDayOfPreviousMonth = LocalDate.now().withDayOfMonth(1).minusDays(1);
        periodResponse = api.get(token, "/api/v1/energy-readings/period-summary?from=%s&to=%s"
                .formatted(firstDayOfPreviousMonth, lastDayOfPreviousMonth));
    }

    @Cuando("consulta un periodo cuya fecha final es anterior a la inicial")
    public void checksAnInvalidPeriod() {
        periodResponse = api.get(token, "/api/v1/energy-readings/period-summary?from=2026-11-20&to=2026-11-01");
    }

    @Entonces("el sistema devuelve un gasto acumulado de S\\/ {double} expresado en soles")
    public void returnsTheAccumulatedCost(double expected) {
        assertThat(summaryResponse.statusCode()).isEqualTo(200);
        JsonNode summary = api.json(summaryResponse);
        assertThat(summary.get("monthToDateCost").decimalValue()).isEqualByComparingTo(BigDecimal.valueOf(expected));
        assertThat(summary.get("currencySymbol").asText()).isEqualTo("S/");
        assertThat(summary.get("monthToDatePeriodStart").asText()).isEqualTo(LocalDate.now().withDayOfMonth(1).toString());
        assertThat(summary.get("monthToDatePeriodEnd").asText()).isEqualTo(LocalDate.now().toString());

        assertThat(periodResponse.statusCode()).isEqualTo(200);
        assertThat(api.json(periodResponse).get("accumulatedCost").decimalValue())
                .isEqualByComparingTo(BigDecimal.valueOf(expected));
    }

    @Entonces("el periodo consultado no incluye las lecturas del mes actual")
    public void thePreviousPeriodExcludesTheCurrentMonth() {
        assertThat(periodResponse.statusCode()).isEqualTo(200);
        JsonNode period = api.json(periodResponse);
        assertThat(period.get("accumulatedCost").decimalValue()).isEqualByComparingTo("0");
        assertThat(period.get("readings").asInt()).isZero();
    }

    @Entonces("el sistema rechaza la consulta indicando que el periodo no es válido")
    public void rejectsTheInvalidPeriod() {
        assertThat(periodResponse.statusCode()).isEqualTo(400);
    }

    private void registerReadingWithCost(String userToken, double cost) {
        // costo / 0.75 = kWh; una lectura de una hora a (kWh x 1000) W produce esos kWh.
        int watts = BigDecimal.valueOf(cost).divide(new BigDecimal("0.75")).multiply(BigDecimal.valueOf(1000)).intValueExact();
        Long deviceId = api.createDevice(userToken, "Horno electrico", watts);
        HttpResponse<String> response = api.registerReading(userToken, deviceId, "Horno electrico", watts, ONE_HOUR);
        assertThat(response.statusCode()).isEqualTo(200);
    }
}
