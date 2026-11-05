package com.electrocorp.electrocorpplatform.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

/**
 * Cliente HTTP de apoyo para los escenarios del Sprint 2 (US-13, US-16, US-18 y US-20).
 * Cada escenario trabaja con un usuario nuevo para no depender de datos de otros escenarios.
 */
@Component
public class SprintTwoApiClient {

    private static final String PASSWORD = "Secur3Pass";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final Environment environment;

    public SprintTwoApiClient(Environment environment) {
        this.environment = environment;
    }

    public String registerAndSignInNewUser() {
        String email = "sprint2-" + UUID.randomUUID() + "@example.com";
        send("POST", "/api/v1/auth/sign-up", null,
                "{\"fullName\":\"Usuario Sprint 2\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        HttpResponse<String> response = send("POST", "/api/v1/auth/sign-in", null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        return json(response).get("token").asText();
    }

    public Long createDevice(String token, String name, int powerWatts) {
        HttpResponse<String> response = send("POST", "/api/v1/devices", token,
                "{\"name\":\"%s\",\"room\":\"Sala\",\"type\":\"Climatizacion\",\"powerWatts\":%d}"
                        .formatted(name, powerWatts));
        return json(response).get("id").asLong();
    }

    public void turnOn(String token, Long deviceId) {
        send("PATCH", "/api/v1/devices/" + deviceId + "/toggle", token, null);
    }

    public HttpResponse<String> registerReading(String token, Long deviceId, String deviceName, int watts) {
        return send("POST", "/api/v1/energy-readings", token,
                "{\"deviceId\":%d,\"deviceName\":\"%s\",\"watts\":%d}".formatted(deviceId, deviceName, watts));
    }

    public HttpResponse<String> registerReading(String token, Long deviceId, String deviceName, int watts, int sampleSeconds) {
        return send("POST", "/api/v1/energy-readings", token,
                "{\"deviceId\":%d,\"deviceName\":\"%s\",\"watts\":%d,\"sampleSeconds\":%d}"
                        .formatted(deviceId, deviceName, watts, sampleSeconds));
    }

    public HttpResponse<String> get(String token, String path) {
        return send("GET", path, token, null);
    }

    public HttpResponse<String> send(String method, String path, String token, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port() + path))
                    .header("Content-Type", "application/json")
                    .method(method, body == null
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(body));
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception exception) {
            throw new IllegalStateException("HTTP request failed: " + method + " " + path, exception);
        }
    }

    private int port() {
        // El puerto aleatorio solo se conoce cuando el servidor ya inicio.
        return environment.getRequiredProperty("local.server.port", Integer.class);
    }

    public JsonNode json(HttpResponse<String> response) {
        try {
            return mapper.readTree(response.body());
        } catch (Exception exception) {
            throw new IllegalStateException("Response is not JSON: " + response.body(), exception);
        }
    }
}
