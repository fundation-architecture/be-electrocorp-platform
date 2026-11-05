package com.electrocorp.electrocorpplatform.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pasos de la prueba de aceptacion de US-20 (Notificacion por consumo elevado).
 */
public class HighConsumptionAlertSteps {

    private static final String DEVICE_NAME = "Aire acondicionado";
    private static final int CONFIGURED_LIMIT_WATTS = 2000;

    @Autowired
    private SprintTwoApiClient api;

    private String token;
    private Long deviceId;

    @Dado("que el usuario tiene una alerta de consumo configurada")
    public void userHasAConsumptionAlertConfigured() {
        token = api.registerAndSignInNewUser();
        deviceId = api.createDevice(token, DEVICE_NAME, 2500);
        HttpResponse<String> rule = api.send("POST", "/api/v1/alerts/rules", token, """
                {
                  "name": "Limite del aire acondicionado",
                  "metric": "WATTS",
                  "conditionType": "GREATER_THAN",
                  "threshold": %d,
                  "level": "WARNING",
                  "scopeType": "DEVICE",
                  "scopeId": "%d"
                }
                """.formatted(CONFIGURED_LIMIT_WATTS, deviceId));
        assertThat(rule.statusCode()).isEqualTo(200);
    }

    @Cuando("un dispositivo supera el límite establecido")
    public void aDeviceExceedsTheLimit() {
        assertThat(api.registerReading(token, deviceId, DEVICE_NAME, 2500).statusCode()).isEqualTo(200);
    }

    @Cuando("un dispositivo registra un consumo menor al límite establecido")
    public void aDeviceStaysBelowTheLimit() {
        assertThat(api.registerReading(token, deviceId, DEVICE_NAME, 600).statusCode()).isEqualTo(200);
    }

    @Entonces("el sistema genera una notificación informando al usuario sobre el consumo elevado y el dispositivo involucrado")
    public void generatesANotificationWithTheDevice() {
        List<JsonNode> alerts = consumptionAlertsOfDevice();

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).get("sourceType").asText()).isEqualTo("DEVICE");
        assertThat(alerts.get(0).get("sourceLabel").asText()).isEqualTo(DEVICE_NAME);
        assertThat(alerts.get(0).get("active").asBoolean()).isTrue();
    }

    @Entonces("el sistema no genera ninguna notificación")
    public void generatesNoNotification() {
        assertThat(consumptionAlertsOfDevice()).isEmpty();
    }

    private List<JsonNode> consumptionAlertsOfDevice() {
        HttpResponse<String> response = api.get(token, "/api/v1/alerts");
        assertThat(response.statusCode()).isEqualTo(200);
        List<JsonNode> result = new ArrayList<>();
        for (JsonNode alert : api.json(response)) {
            String threadKey = alert.path("threadKey").asText();
            boolean sameDevice = String.valueOf(deviceId).equals(alert.path("sourceId").asText());
            if (sameDevice && (threadKey.startsWith("ENERGY:") || threadKey.startsWith("RULE:"))) {
                result.add(alert);
            }
        }
        return result;
    }
}
