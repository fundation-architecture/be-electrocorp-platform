package com.electrocorp.electrocorpplatform.energymonitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-22 (QA-02): verificacion del resumen de consumo con PostgreSQL 16 y Redis 7 reales.
 * Comprueba la clave consumption:{userId}:summary, su TTL de 30 s, la invalidacion al registrar una
 * lectura y mide el tiempo de respuesta con y sin cache. Requiere Docker: en una maquina sin Docker la
 * prueba se omite; en GitHub Actions (T-19) se ejecuta en cada push.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SummaryCachePostgresRedisIntegrationTest {

    private static final int READINGS = 200;
    private static final int MEASURED_REQUESTS = 30;
    private static final long QA_02_LIMIT_MILLIS = 5000;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void realInfrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("electrocorp.energy.summary-cache.enabled", () -> "true");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void summaryIsCachedWithA30SecondTtlAndInvalidatedByANewReading() throws Exception {
        String token = signUpAndSignIn();
        long deviceId = createDevice(token);
        registerReading(token, deviceId, 1000);

        String key = summaryKeyCreatedBy(token);
        Long ttl = redisTemplate.getExpire(key);
        assertThat(ttl).isBetween(1L, 30L);

        registerReading(token, deviceId, 1100);

        assertThat(redisTemplate.hasKey(key)).isFalse();
        assertThat(summary(token).statusCode()).isEqualTo(200);
        assertThat(redisTemplate.hasKey(key)).isTrue();
    }

    @Test
    void summaryRespondsWithinQa02WithAndWithoutCache() throws Exception {
        String token = signUpAndSignIn();
        long deviceId = createDevice(token);
        for (int i = 0; i < READINGS; i++) {
            registerReading(token, deviceId, 300 + (i % 50));
        }
        String key = summaryKeyCreatedBy(token);

        List<Long> withoutCache = new ArrayList<>();
        for (int i = 0; i < MEASURED_REQUESTS; i++) {
            redisTemplate.delete(key);
            withoutCache.add(timed(token));
        }
        List<Long> withCache = new ArrayList<>();
        summary(token);
        for (int i = 0; i < MEASURED_REQUESTS; i++) {
            withCache.add(timed(token));
        }

        String report = """
                # QA-02 - Resumen del panel con PostgreSQL y Redis reales (T-22)

                Ejecutado: %s
                Entorno: Testcontainers con %s y %s.
                Datos: 1 usuario, 1 dispositivo, %d lecturas. %d peticiones por escenario.

                | Escenario | Promedio (ms) | p95 (ms) | Maximo (ms) | Limite QA-02 |
                |-----------|---------------|----------|-------------|--------------|
                | Sin cache (clave borrada antes de cada consulta) | %.1f | %d | %d | < %d ms |
                | Con cache (Redis) | %.1f | %d | %d | < %d ms |
                """.formatted(
                LocalDateTime.now().withNano(0), POSTGRES.getDockerImageName(), REDIS.getDockerImageName(),
                READINGS, MEASURED_REQUESTS,
                average(withoutCache), percentile95(withoutCache), Collections.max(withoutCache), QA_02_LIMIT_MILLIS,
                average(withCache), percentile95(withCache), Collections.max(withCache), QA_02_LIMIT_MILLIS
        );
        Path output = Path.of("target", "qa", "QA-02-postgres-redis.md");
        Files.createDirectories(output.getParent());
        Files.writeString(output, report);
        System.out.println(report);

        assertThat(percentile95(withoutCache)).isLessThan(QA_02_LIMIT_MILLIS);
        assertThat(percentile95(withCache)).isLessThan(QA_02_LIMIT_MILLIS);
    }

    /** Consulta el resumen y devuelve la clave de Redis que creo esa consulta (consumption:{userId}:summary). */
    private String summaryKeyCreatedBy(String token) throws Exception {
        Set<String> before = new HashSet<>(redisTemplate.keys("consumption:*:summary"));
        assertThat(summary(token).statusCode()).isEqualTo(200);
        Set<String> created = new HashSet<>(redisTemplate.keys("consumption:*:summary"));
        created.removeAll(before);
        assertThat(created).hasSize(1);
        String key = created.iterator().next();
        assertThat(key).matches("consumption:\\d+:summary");
        return key;
    }

    private String signUpAndSignIn() throws Exception {
        String email = "redis-" + UUID.randomUUID() + "@example.com";
        send("POST", "/api/v1/auth/sign-up", null,
                "{\"fullName\":\"Prueba Redis\",\"email\":\"%s\",\"password\":\"Secur3Pass\"}".formatted(email));
        return json(send("POST", "/api/v1/auth/sign-in", null,
                "{\"email\":\"%s\",\"password\":\"Secur3Pass\"}".formatted(email))).get("token").asText();
    }

    private long createDevice(String token) throws Exception {
        return json(send("POST", "/api/v1/devices", token,
                "{\"name\":\"Refrigeradora\",\"room\":\"Cocina\",\"type\":\"Linea blanca\",\"powerWatts\":350}"))
                .get("id").asLong();
    }

    private void registerReading(String token, long deviceId, int watts) throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/energy-readings", token,
                "{\"deviceId\":%d,\"deviceName\":\"Refrigeradora\",\"watts\":%d,\"sampleSeconds\":60}".formatted(deviceId, watts));
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> summary(String token) throws Exception {
        return send("GET", "/api/v1/energy-readings/dashboard-summary", token, null);
    }

    private long timed(String token) throws Exception {
        long start = System.nanoTime();
        HttpResponse<String> response = summary(token);
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        assertThat(response.statusCode()).isEqualTo(200);
        return elapsed;
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
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

    private double average(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private long percentile95(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(Math.max(0, (int) Math.ceil(0.95 * sorted.size()) - 1));
    }
}
