package com.electrocorp.electrocorpplatform.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Business "Given"/"Then" steps for the IAM feature. The generic HTTP steps
 * live in {@link CommonHttpSteps}.
 */
public class IamSteps {

    @Autowired
    private TestContext ctx;

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private HttpResponse<String> postJson(String path, String body) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("HTTP request failed", e);
        }
    }

    @Given("an account exists for email {string} with password {string}")
    public void anAccountExists(String email, String password) {
        String body = "{\"fullName\":\"Test User\",\"email\":\"%s\",\"password\":\"%s\"}"
                .formatted(email, password);
        // Idempotent: a 409 (already exists) is acceptable for test setup.
        postJson("/api/v1/auth/sign-up", body);
    }

    @Given("an account already exists for email {string}")
    public void anAccountAlreadyExists(String email) {
        anAccountExists(email, "Secur3Pass");
    }

    @Given("no account exists for email {string}")
    public void noAccountExists(String email) {
        // Fresh in-memory schema per run: nothing to delete.
    }

    @Given("the client is authenticated as {string}")
    public void theClientIsAuthenticatedAs(String email) throws Exception {
        anAccountExists(email, "Secur3Pass");
        ctx.lastAuthEmail = email;
        HttpResponse<String> response = postJson("/api/v1/auth/sign-in",
                "{\"email\":\"%s\",\"password\":\"Secur3Pass\"}".formatted(email));
        ctx.jwtToken = mapper.readTree(response.body()).get("token").asText();
    }

    @Then("a subsequent sign in with the same credentials returns status code {int}")
    public void subsequentSignIn(int expected) {
        HttpResponse<String> response = postJson("/api/v1/auth/sign-in",
                "{\"email\":\"%s\",\"password\":\"Secur3Pass\"}".formatted(ctx.lastAuthEmail));
        assertThat(response.statusCode()).isEqualTo(expected);
    }
}
