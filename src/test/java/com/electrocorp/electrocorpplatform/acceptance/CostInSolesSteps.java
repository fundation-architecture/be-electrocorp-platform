package com.electrocorp.electrocorpplatform.acceptance;

import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.TariffProvider;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pasos de la prueba de aceptacion de US-16 (Visualizacion del costo en soles).
 * Los dispositivos se dejan apagados para que el planificador no agregue lecturas durante el escenario.
 */
public class CostInSolesSteps {

    private static final int ONE_HOUR_IN_SECONDS = 3600;

    @Autowired
    private SprintTwoApiClient api;

    @Autowired
    private TariffProvider tariffProvider;

    private String token;
    private HttpResponse<String> summaryResponse;

    @Dado("que el sistema dispone de información sobre el consumo energético registrado")
    public void thereIsRegisteredConsumption() {
        token = api.registerAndSignInNewUser();
        Long deviceId = api.createDevice(token, "Terma electrica", 1500);
        HttpResponse<String> reading = api.registerReading(token, deviceId, "Terma electrica", 1500, ONE_HOUR_IN_SECONDS);
        assertThat(reading.statusCode()).isEqualTo(200);
    }

    @Dado("^que la tarifa configurada es de S/ (\\d+\\.\\d+) por kWh y el usuario consumió (\\d+) kWh hoy$")
    public void tariffIsConfiguredAndUserConsumed(String tariff, String kilowattHours) {
        assertThat(tariffProvider.tariffPerKilowattHour()).isEqualByComparingTo(tariff);
        token = api.registerAndSignInNewUser();
        Long deviceId = api.createDevice(token, "Horno electrico", 1000);
        // 1000 W durante una hora = 1 kWh por lectura.
        for (int hour = 0; hour < Integer.parseInt(kilowattHours); hour++) {
            HttpResponse<String> reading = api.registerReading(token, deviceId, "Horno electrico", 1000, ONE_HOUR_IN_SECONDS);
            assertThat(reading.statusCode()).isEqualTo(200);
        }
    }

    @Cuando("el usuario consulta el detalle de su consumo")
    public void theUserChecksTheConsumptionDetail() {
        summaryResponse = api.get(token, "/api/v1/energy-readings/dashboard-summary");
    }

    @Cuando("consulta el detalle de su consumo")
    public void checksTheConsumptionDetail() {
        theUserChecksTheConsumptionDetail();
    }

    @Entonces("el sistema calcula y muestra el costo estimado correspondiente expresado en soles \\(S\\/)")
    public void showsTheEstimatedCostInSoles() {
        assertThat(summaryResponse.statusCode()).isEqualTo(200);
        JsonNode summary = api.json(summaryResponse);
        assertThat(summary.get("currency").asText()).isEqualTo("PEN");
        assertThat(summary.get("currencySymbol").asText()).isEqualTo("S/");
        assertThat(summary.get("todayEstimatedCost").decimalValue()).isPositive();
        assertThat(summary.get("costPerHour")).isNotNull();
    }

    @Entonces("^el costo estimado mostrado es de S/ (\\d+\\.\\d{2})$")
    public void theEstimatedCostIs(String expectedCost) {
        assertThat(summaryResponse.statusCode()).isEqualTo(200);
        JsonNode summary = api.json(summaryResponse);
        assertThat(summary.get("todayKilowattHours").decimalValue()).isEqualByComparingTo("2");
        assertThat(summary.get("todayEstimatedCost").decimalValue()).isEqualByComparingTo(new BigDecimal(expectedCost));
    }
}
