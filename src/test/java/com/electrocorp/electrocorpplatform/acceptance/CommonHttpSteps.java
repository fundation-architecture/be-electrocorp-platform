package com.electrocorp.electrocorpplatform.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonHttpSteps {

    @Autowired private TestRestTemplate rest;   // cliente HTTP real de Spring Test
    @Autowired private TestContext ctx;
    @LocalServerPort private int port;
    private final ObjectMapper mapper = new ObjectMapper();
    private String basePath = "";

    private String url(String path) {
        return "http://localhost:" + port + basePath + path;
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (ctx.jwtToken != null) h.setBearerAuth(ctx.jwtToken);
        return h;
    }

    @Given("the RESTful API is available at base path {string}")
    public void setBasePath(String path) { this.basePath = path; }

    @When("the client sends a GET request to {string}")
    public void get(String path) {
        ctx.lastResponse = rest.exchange(url(path), HttpMethod.GET,
                new HttpEntity<>(headers()), String.class);
    }

    @When("the client sends a POST request to {string} with body:")
    public void post(String path, String body) {
        ctx.lastResponse = rest.exchange(url(path), HttpMethod.POST,
                new HttpEntity<>(body, headers()), String.class);
    }

    @When("the client sends a PUT request to {string} with body:")
    public void put(String path, String body) {
        ctx.lastResponse = rest.exchange(url(path), HttpMethod.PUT,
                new HttpEntity<>(body, headers()), String.class);
    }

    @When("the client sends a PATCH request to {string} with body:")
    public void patch(String path, String body) {
        ctx.lastResponse = rest.exchange(url(path), HttpMethod.PATCH,
                new HttpEntity<>(body, headers()), String.class);
    }

    @When("the client sends a PATCH request to {string}")
    public void patchNoBody(String path) {
        ctx.lastResponse = rest.exchange(url(path), HttpMethod.PATCH,
                new HttpEntity<>(headers()), String.class);
    }

    @When("the client sends a DELETE request to {string}")
    public void delete(String path) {
        ctx.lastResponse = rest.exchange(url(path), HttpMethod.DELETE,
                new HttpEntity<>(headers()), String.class);
    }

    @Then("the response status code is {int}")
    public void assertStatus(int expected) {
        assertThat(ctx.lastResponse.getStatusCode().value()).isEqualTo(expected);
    }

    @Then("the response body contains {string} equal to {string}")
    public void assertField(String jsonPath, String expected) throws Exception {
        JsonNode node = mapper.readTree(ctx.lastResponse.getBody());
        for (String part : jsonPath.split("\\.")) node = node.get(part); // soporta "user.email"
        assertThat(node.asText()).isEqualTo(expected);
    }

    @Then("the response body contains a non-empty {string}")
    public void assertNonEmpty(String field) throws Exception {
        JsonNode node = mapper.readTree(ctx.lastResponse.getBody()).get(field);
        assertThat(node).isNotNull();
        assertThat(node.asText()).isNotBlank();
    }
}
