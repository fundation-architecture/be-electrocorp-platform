package com.electrocorp.electrocorpplatform.shared.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-40: los endpoints de Energy Monitoring y Notifications quedan documentados en el
 * contrato OpenAPI que muestra Swagger UI (/swagger-ui.html).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OpenApiDocumentationTest {

    @LocalServerPort
    private int port;

    @Test
    void sprintTwoEndpointsAreDocumented() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        JsonNode paths = new ObjectMapper().readTree(response.body()).get("paths");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(operation(paths, "/api/v1/energy-readings/dashboard-summary", "get").get("summary").asText())
                .isEqualTo("Consultar el resumen del panel de consumo");
        assertThat(operation(paths, "/api/v1/energy-readings/dashboard-summary", "get").get("tags").get(0).asText())
                .isEqualTo("Energy Monitoring");
        assertThat(operation(paths, "/api/v1/energy-readings", "post").get("summary").asText())
                .isEqualTo("Registrar una lectura de consumo");
        assertThat(operation(paths, "/api/v1/alerts", "get").get("tags").get(0).asText())
                .isEqualTo("Notifications");
        assertThat(operation(paths, "/api/v1/alerts/rules", "post").get("summary").asText())
                .isEqualTo("Crear una regla de alerta");
        assertThat(operation(paths, "/api/v1/notifications/preferences", "put")).isNotNull();
    }

    private JsonNode operation(JsonNode paths, String path, String method) {
        JsonNode pathItem = paths.get(path);
        assertThat(pathItem).as("path %s documented", path).isNotNull();
        return pathItem.get(method);
    }
}
