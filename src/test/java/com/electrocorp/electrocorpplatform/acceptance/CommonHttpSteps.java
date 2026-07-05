package com.electrocorp.electrocorpplatform.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generic HTTP step definitions shared by every .feature file. Uses the JDK
 * HttpClient (no Spring test-client dependency) against the running server.
 */
public class CommonHttpSteps {

    @Autowired
    private TestContext ctx;

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private String basePath = "";

    private HttpRequest.Builder request(String path, boolean withAuth) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + basePath + path))
                .header("Content-Type", "application/json");
        if (withAuth && ctx.jwtToken != null) {
            builder.header("Authorization", "Bearer " + ctx.jwtToken);
        }
        return builder;
    }

    private void send(HttpRequest req) {
        try {
            ctx.lastResponse = client.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("HTTP request failed", e);
        }
    }

    @Given("the RESTful API is available at base path {string}")
    public void setBasePath(String path) {
        this.basePath = path;
    }

    @When("the client sends a GET request to {string}")
    public void get(String path) {
        send(request(path, true).GET().build());
    }

    @When("the client sends a GET request to {string} without an Authorization header")
    public void getWithoutAuth(String path) {
        send(request(path, false).GET().build());
    }

    @When("the client sends a POST request to {string} with body:")
    public void post(String path, String body) {
        send(request(path, true).POST(HttpRequest.BodyPublishers.ofString(body)).build());
    }

    @When("the client sends a PUT request to {string} with body:")
    public void put(String path, String body) {
        send(request(path, true).PUT(HttpRequest.BodyPublishers.ofString(body)).build());
    }

    @When("the client sends a PATCH request to {string} with body:")
    public void patch(String path, String body) {
        send(request(path, true).method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build());
    }

    @When("the client sends a PATCH request to {string}")
    public void patchNoBody(String path) {
        send(request(path, true).method("PATCH", HttpRequest.BodyPublishers.noBody()).build());
    }

    @When("the client sends a DELETE request to {string}")
    public void delete(String path) {
        send(request(path, true).DELETE().build());
    }

    @Then("the response status code is {int}")
    public void assertStatus(int expected) {
        assertThat(ctx.lastResponse.statusCode()).isEqualTo(expected);
    }

    @Then("the response body contains {string} equal to {string}")
    public void assertField(String jsonPath, String expected) throws Exception {
        JsonNode node = mapper.readTree(ctx.lastResponse.body());
        for (String part : jsonPath.split("\\.")) {
            node = node.get(part);
        }
        assertThat(node).as("field %s", jsonPath).isNotNull();
        assertThat(node.asText()).isEqualTo(expected);
    }

    @Then("the response body contains a non-empty {string}")
    public void assertNonEmpty(String field) throws Exception {
        JsonNode node = mapper.readTree(ctx.lastResponse.body()).get(field);
        assertThat(node).isNotNull();
        assertThat(node.asText()).isNotBlank();
    }

    @Then("the response body is a JSON array")
    public void assertIsArray() throws Exception {
        assertThat(mapper.readTree(ctx.lastResponse.body()).isArray()).isTrue();
    }

    @Then("the response body is a list containing a plan with code {string}")
    public void assertListContainsPlan(String code) throws Exception {
        JsonNode array = mapper.readTree(ctx.lastResponse.body());
        boolean found = false;
        for (JsonNode element : array) {
            if (code.equals(element.path("code").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found).as("plan with code %s present in list", code).isTrue();
    }
}
