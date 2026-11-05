package com.electrocorp.electrocorpplatform.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-37: flujo de extremo a extremo de la alerta por consumo elevado.
 * POST /api/v1/energy-readings -> EnergyThresholdExceededEvent -> integration event ->
 * NotificationIntegrationEventHandler -> GET /api/v1/alerts.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HighConsumptionAlertFlowIntegrationTest {

    private static final String DEVICE_NAME = "Aire acondicionado";

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private String token;
    private Long deviceId;

    @BeforeEach
    void registerUserWithDevice() throws Exception {
        String email = "alert-flow-" + UUID.randomUUID() + "@example.com";
        send("POST", "/api/v1/auth/sign-up",
                "{\"fullName\":\"Flujo Alerta\",\"email\":\"%s\",\"password\":\"Secur3Pass\"}".formatted(email));
        token = json(send("POST", "/api/v1/auth/sign-in",
                "{\"email\":\"%s\",\"password\":\"Secur3Pass\"}".formatted(email))).get("token").asText();
        deviceId = json(send("POST", "/api/v1/devices",
                "{\"name\":\"%s\",\"room\":\"Sala\",\"type\":\"Climatizacion\",\"powerWatts\":2500}".formatted(DEVICE_NAME)))
                .get("id").asLong();
    }

    @Test
    void readingAboveTheLimitCreatesOneAlertReturnedByGetAlerts() throws Exception {
        HttpResponse<String> reading = registerReading(2500);

        assertThat(reading.statusCode()).isEqualTo(200);
        assertThat(json(reading).get("status").asText()).isEqualTo("HIGH");

        HttpResponse<String> alertsResponse = send("GET", "/api/v1/alerts", null);
        List<JsonNode> consumptionAlerts = consumptionAlertsOfDevice(json(alertsResponse));

        assertThat(alertsResponse.statusCode()).isEqualTo(200);
        assertThat(consumptionAlerts).hasSize(1);
        JsonNode alert = consumptionAlerts.get(0);
        assertThat(alert.get("sourceType").asText()).isEqualTo("DEVICE");
        assertThat(alert.get("sourceLabel").asText()).isEqualTo(DEVICE_NAME);
        assertThat(alert.get("title").asText()).contains(DEVICE_NAME);
        assertThat(alert.get("repeatCount").asInt()).isEqualTo(1);
    }

    @Test
    void readingBelowTheLimitDoesNotCreateAnAlert() throws Exception {
        HttpResponse<String> reading = registerReading(600);

        assertThat(reading.statusCode()).isEqualTo(200);
        assertThat(json(reading).get("status").asText()).isEqualTo("NORMAL");
        assertThat(consumptionAlertsOfDevice(json(send("GET", "/api/v1/alerts", null)))).isEmpty();
    }

    private HttpResponse<String> registerReading(int watts) throws Exception {
        return send("POST", "/api/v1/energy-readings",
                "{\"deviceId\":%d,\"deviceName\":\"%s\",\"watts\":%d}".formatted(deviceId, DEVICE_NAME, watts));
    }

    private List<JsonNode> consumptionAlertsOfDevice(JsonNode alerts) {
        List<JsonNode> result = new ArrayList<>();
        for (JsonNode alert : alerts) {
            boolean sameDevice = String.valueOf(deviceId).equals(alert.path("sourceId").asText());
            boolean consumptionAlert = alert.path("threadKey").asText().startsWith("ENERGY:")
                    || alert.path("threadKey").asText().startsWith("RULE:");
            if (sameDevice && consumptionAlert) {
                result.add(alert);
            }
        }
        return result;
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) throws Exception {
        return mapper.readTree(response.body());
    }
}
