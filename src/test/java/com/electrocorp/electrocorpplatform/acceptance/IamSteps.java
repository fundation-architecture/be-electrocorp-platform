package com.electrocorp.electrocorpplatform.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Business "Given" steps for the IAM feature. The generic HTTP "When"/"Then"
 * steps live in {@link CommonHttpSteps}.
 */
public class IamSteps {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestContext ctx;

    @LocalServerPort
    private int port;

    private final ObjectMapper mapper = new ObjectMapper();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Given("an account exists for email {string} with password {string}")
    public void anAccountExists(String email, String password) {
        String body = """
            {"fullName":"Test User","email":"%s","password":"%s"}
            """.formatted(email, password);
        // Idempotent: a 409 (already exists) is fine for test setup.
        rest.postForEntity(url("/api/v1/auth/sign-up"),
                new HttpEntity<>(body, jsonHeaders()), String.class);
    }

    @Given("an account already exists for email {string}")
    public void anAccountAlreadyExists(String email) {
        anAccountExists(email, "Secur3Pass");
    }

    @Given("no account exists for email {string}")
    public void noAccountExists(String email) {
        // With a fresh in-memory schema per run there is nothing to delete.
    }

    @Given("the client is authenticated as {string}")
    public void theClientIsAuthenticatedAs(String email) throws Exception {
        anAccountExists(email, "Secur3Pass");
        String body = """
            {"email":"%s","password":"Secur3Pass"}
            """.formatted(email);
        ResponseEntity<String> response = rest.postForEntity(url("/api/v1/auth/sign-in"),
                new HttpEntity<>(body, jsonHeaders()), String.class);
        ctx.jwtToken = mapper.readTree(response.getBody()).get("token").asText();
    }
}
