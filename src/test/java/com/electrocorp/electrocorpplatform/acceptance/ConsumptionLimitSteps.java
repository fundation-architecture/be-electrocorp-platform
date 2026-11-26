package com.electrocorp.electrocorpplatform.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pasos de las pruebas de aceptacion de US-19 (limite de consumo) y US-21 (centro de notificaciones).
 * El limite configurado (1000 W) es menor que el limite general de la plataforma (1800 W), de modo que
 * una alerta por una lectura de 1001 W solo aparece si se aplica el limite del usuario.
 */
public class ConsumptionLimitSteps {

    private static final String DEVICE_NAME = "Aire acondicionado";

    @Autowired
    private SprintTwoApiClient api;

    private String token;
    private Long deviceId;
    private HttpResponse<String> lastResponse;
    private int consumptionAlertsBefore;

    @Dado("que el usuario está autenticado y tiene un dispositivo vinculado")
    public void userIsAuthenticatedWithADevice() {
        token = api.registerAndSignInNewUser();
        deviceId = api.createDevice(token, DEVICE_NAME, 2500);
    }

    @Cuando("configura un límite de potencia de {int} W para ese dispositivo")
    public void configuresAPowerLimit(int watts) {
        lastResponse = createLimit(watts);
    }

    @Entonces("el sistema guarda el límite en watts asociado al dispositivo")
    public void savesTheLimit() {
        assertThat(lastResponse.statusCode()).isEqualTo(200);
        JsonNode rule = api.json(lastResponse);
        assertThat(rule.get("metric").asText()).isEqualTo("WATTS");
        assertThat(rule.get("threshold").decimalValue()).isEqualByComparingTo("1000");
        assertThat(rule.get("scopeType").asText()).isEqualTo("DEVICE");
        assertThat(rule.get("scopeId").asText()).isEqualTo(String.valueOf(deviceId));
    }

    @Y("mantiene activa la regla asociada al dispositivo")
    public void keepsTheRuleActive() {
        JsonNode rules = api.json(api.get(token, "/api/v1/alerts/rules"));
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).get("enabled").asBoolean()).isTrue();
    }

    @Dado("que el dispositivo tiene una regla activa con un límite de {int} W")
    public void theDeviceHasAnActiveLimit(int watts) {
        userIsAuthenticatedWithADevice();
        assertThat(createLimit(watts).statusCode()).isEqualTo(200);
    }

    @Y("no existen alertas previas de consumo para el dispositivo")
    public void thereAreNoPreviousAlerts() {
        consumptionAlertsBefore = consumptionAlertsOfDevice(token, deviceId);
        assertThat(consumptionAlertsBefore).isZero();
    }

    @Cuando("se registra una lectura de {int} W para ese dispositivo")
    public void aReadingIsRegistered(int watts) {
        lastResponse = api.registerReading(token, deviceId, DEVICE_NAME, watts);
        assertThat(lastResponse.statusCode()).isEqualTo(200);
    }

    @Entonces("se generan {int} alertas nuevas por consumo elevado para esa lectura")
    public void alertsAreGenerated(int expectedAlerts) {
        assertThat(consumptionAlertsOfDevice(token, deviceId) - consumptionAlertsBefore).isEqualTo(expectedAlerts);
    }

    @Cuando("intenta configurar un límite de potencia negativo")
    public void triesANegativeLimit() {
        lastResponse = createLimit(-100);
    }

    @Entonces("el sistema rechaza la configuración")
    public void rejectsTheConfiguration() {
        assertThat(lastResponse.statusCode()).isEqualTo(400);
    }

    @Y("no crea ninguna regla para el dispositivo")
    public void doesNotCreateARule() {
        assertThat(api.json(api.get(token, "/api/v1/alerts/rules"))).isEmpty();
    }

    private HttpResponse<String> createLimit(int watts) {
        return api.send("POST", "/api/v1/alerts/rules", token, """
                {
                  "name": "Limite del aire acondicionado",
                  "metric": "WATTS",
                  "conditionType": "GREATER_THAN",
                  "threshold": %d,
                  "level": "WARNING",
                  "scopeType": "DEVICE",
                  "scopeId": "%d"
                }
                """.formatted(watts, deviceId));
    }

    private int consumptionAlertsOfDevice(String userToken, Long device) {
        int count = 0;
        for (JsonNode alert : api.json(api.get(userToken, "/api/v1/alerts"))) {
            if (isConsumptionAlertOf(alert, device)) {
                count++;
            }
        }
        return count;
    }

    private boolean isConsumptionAlertOf(JsonNode alert, Long device) {
        String threadKey = alert.path("threadKey").asText();
        return String.valueOf(device).equals(alert.path("sourceId").asText())
                && (threadKey.startsWith("ENERGY:") || threadKey.startsWith("RULE:"));
    }
}
