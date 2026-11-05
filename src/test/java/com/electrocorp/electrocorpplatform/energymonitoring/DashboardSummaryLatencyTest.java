package com.electrocorp.electrocorpplatform.energymonitoring;

import com.electrocorp.electrocorpplatform.energymonitoring.application.ports.ConsumptionSummaryCache;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.EnergyDashboardSummaryResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-42 (QA-02): mide el tiempo de respuesta de GET /api/v1/energy-readings/dashboard-summary
 * con y sin cache y registra el resultado en target/qa/QA-02-latencia-resumen.md.
 * La cache se reemplaza por una version en memoria con el mismo puerto, para medir sin depender
 * de un servidor Redis; la medicion contra Redis real se describe en docs/qa/QA-02-latencia-resumen.md.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DashboardSummaryLatencyTest {

    private static final int READINGS = 300;
    private static final int WARM_UP_REQUESTS = 5;
    private static final int MEASURED_REQUESTS = 40;
    private static final long QA_02_LIMIT_MILLIS = 5000;

    @LocalServerPort
    private int port;

    @Autowired
    private SwitchableInMemoryCache cache;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void dashboardSummaryRespondsWithinQa02WithAndWithoutCache() throws Exception {
        String token = registerUserWithReadings();

        cache.enabled = false;
        List<Long> withoutCache = measure(token);
        cache.enabled = true;
        cache.store.clear();
        List<Long> withCache = measure(token);

        String report = report(withoutCache, withCache);
        Path output = Path.of("target", "qa", "QA-02-latencia-resumen.md");
        Files.createDirectories(output.getParent());
        Files.writeString(output, report);
        System.out.println(report);

        assertThat(percentile(withoutCache, 95)).isLessThan(QA_02_LIMIT_MILLIS);
        assertThat(percentile(withCache, 95)).isLessThan(QA_02_LIMIT_MILLIS);
    }

    private String registerUserWithReadings() throws Exception {
        String email = "latency-" + UUID.randomUUID() + "@example.com";
        send("POST", "/api/v1/auth/sign-up", null,
                "{\"fullName\":\"Medicion QA-02\",\"email\":\"%s\",\"password\":\"Secur3Pass\"}".formatted(email));
        String token = mapper.readTree(send("POST", "/api/v1/auth/sign-in", null,
                "{\"email\":\"%s\",\"password\":\"Secur3Pass\"}".formatted(email)).body()).get("token").asText();
        long deviceId = mapper.readTree(send("POST", "/api/v1/devices", token,
                "{\"name\":\"Refrigeradora\",\"room\":\"Cocina\",\"type\":\"Linea blanca\",\"powerWatts\":350}").body())
                .get("id").asLong();
        for (int i = 0; i < READINGS; i++) {
            send("POST", "/api/v1/energy-readings", token,
                    "{\"deviceId\":%d,\"deviceName\":\"Refrigeradora\",\"watts\":%d,\"sampleSeconds\":60}"
                            .formatted(deviceId, 300 + (i % 50)));
        }
        return token;
    }

    private List<Long> measure(String token) throws Exception {
        for (int i = 0; i < WARM_UP_REQUESTS; i++) {
            summary(token);
        }

        List<Long> durations = new ArrayList<>();
        for (int i = 0; i < MEASURED_REQUESTS; i++) {
            long start = System.nanoTime();
            HttpResponse<String> response = summary(token);
            durations.add((System.nanoTime() - start) / 1_000_000);
            assertThat(response.statusCode()).isEqualTo(200);
        }
        return durations;
    }

    private HttpResponse<String> summary(String token) throws Exception {
        return send("GET", "/api/v1/energy-readings/dashboard-summary", token, null);
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String report(List<Long> withoutCache, List<Long> withCache) {
        return """
                # QA-02 - Latencia del resumen del panel

                Ejecutado: %s
                Endpoint: GET /api/v1/energy-readings/dashboard-summary
                Datos: 1 usuario, 1 dispositivo, %d lecturas. %d peticiones medidas por escenario (%d de calentamiento).

                | Escenario | Promedio (ms) | p95 (ms) | Maximo (ms) | Limite QA-02 |
                |-----------|---------------|----------|-------------|--------------|
                | Sin cache | %.1f | %d | %d | < %d ms |
                | Con cache | %.1f | %d | %d | < %d ms |
                """.formatted(
                LocalDateTime.now().withNano(0), READINGS, MEASURED_REQUESTS, WARM_UP_REQUESTS,
                average(withoutCache), percentile(withoutCache, 95), Collections.max(withoutCache), QA_02_LIMIT_MILLIS,
                average(withCache), percentile(withCache, 95), Collections.max(withCache), QA_02_LIMIT_MILLIS
        );
    }

    private double average(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private long percentile(List<Long> values, int percentile) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }

    @TestConfiguration
    static class LatencyTestConfiguration {
        @Bean
        @Primary
        SwitchableInMemoryCache switchableInMemoryCache() {
            return new SwitchableInMemoryCache();
        }
    }

    /** Cache en memoria con el mismo contrato que Redis; se puede apagar para medir sin cache. */
    static class SwitchableInMemoryCache implements ConsumptionSummaryCache {
        final Map<String, EnergyDashboardSummaryResource> store = new ConcurrentHashMap<>();
        volatile boolean enabled = true;

        @Override
        public Optional<EnergyDashboardSummaryResource> get(Long userId) {
            return enabled ? Optional.ofNullable(store.get(ConsumptionSummaryCache.keyFor(userId))) : Optional.empty();
        }

        @Override
        public void put(Long userId, EnergyDashboardSummaryResource summary) {
            if (enabled) {
                store.put(ConsumptionSummaryCache.keyFor(userId), summary);
            }
        }

        @Override
        public void evict(Long userId) {
            store.remove(ConsumptionSummaryCache.keyFor(userId));
        }
    }
}
