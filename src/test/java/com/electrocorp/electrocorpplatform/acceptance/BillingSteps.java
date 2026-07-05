package com.electrocorp.electrocorpplatform.acceptance;

import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Business "Given" steps for the Billing feature. Authentication is handled by
 * {@link IamSteps} and the generic HTTP steps by {@link CommonHttpSteps}.
 */
public class BillingSteps {

    @Autowired
    private TestContext ctx;

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private void checkout(String planCode) {
        try {
            String body = ("{\"planCode\":\"%s\",\"holderName\":\"Carlos Mendoza\","
                    + "\"cardNumber\":\"4111111111111111\",\"expirationDate\":\"12/29\","
                    + "\"cvv\":\"123\"}").formatted(planCode);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/billing/subscriptions/checkout"))
                    .header("Content-Type", "application/json");
            if (ctx.jwtToken != null) {
                builder.header("Authorization", "Bearer " + ctx.jwtToken);
            }
            HttpRequest req = builder
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            client.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("HTTP request failed", e);
        }
    }

    @Given("the billing plan catalog is initialized")
    public void theBillingPlanCatalogIsInitialized() {
        // BillingPlanCatalogInitializer seeds the plans on application startup.
    }

    @Given("the client has an active subscription with plan {string}")
    public void theClientHasAnActiveSubscriptionWithPlan(String planCode) {
        checkout(planCode);
    }

    @Given("the client has an active subscription with id {long}")
    public void theClientHasAnActiveSubscriptionWithId(Long id) {
        checkout("ENTERPRISE");
    }

    @Given("the client has at least one paid invoice")
    public void theClientHasAtLeastOnePaidInvoice() {
        checkout("ENTERPRISE");
    }
}
