package com.electrocorp.electrocorpplatform.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pasos de la prueba de aceptacion de US-13 (Visualizacion de consumo energetico actual).
 */
public class CurrentConsumptionSteps {

    @Autowired
    private SprintTwoApiClient api;

    private String token;
    private HttpResponse<String> dashboardResponse;

    @Dado("que el usuario tiene dispositivos IoT vinculados y enviando información de consumo")
    public void userHasDevicesSendingConsumption() {
        token = api.registerAndSignInNewUser();
        Long deviceId = api.createDevice(token, "Aire acondicionado", 1200);
        api.turnOn(token, deviceId);
        HttpResponse<String> reading = api.registerReading(token, deviceId, "Aire acondicionado", 1150);
        assertThat(reading.statusCode()).isEqualTo(200);
    }

    @Dado("que el usuario tiene dispositivos vinculados que todavía no han enviado lecturas")
    public void userHasDevicesWithoutReadings() {
        token = api.registerAndSignInNewUser();
        api.createDevice(token, "Lampara de escritorio", 60);
    }

    @Cuando("accede al panel de monitoreo")
    public void accessesTheMonitoringDashboard() {
        dashboardResponse = api.get(token, "/api/v1/energy-readings/dashboard-summary");
    }

    @Entonces("el sistema muestra el consumo energético actual registrado por los dispositivos")
    public void showsTheCurrentConsumption() {
        assertThat(dashboardResponse.statusCode()).isEqualTo(200);
        JsonNode summary = api.json(dashboardResponse);
        assertThat(summary.get("currentWatts").decimalValue()).isPositive();
        assertThat(summary.get("todayKilowattHours")).isNotNull();
        assertThat(summary.get("activeDevices").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Entonces("el sistema muestra un consumo actual de {int} W sin devolver error")
    public void showsCurrentConsumptionOf(int expectedWatts) {
        assertThat(dashboardResponse.statusCode()).isEqualTo(200);
        JsonNode summary = api.json(dashboardResponse);
        assertThat(summary.get("currentWatts").decimalValue()).isEqualByComparingTo(String.valueOf(expectedWatts));
        assertThat(summary.get("todayKilowattHours").decimalValue()).isEqualByComparingTo("0");
    }
}
