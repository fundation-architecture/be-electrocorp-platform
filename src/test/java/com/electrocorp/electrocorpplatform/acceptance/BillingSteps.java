package com.electrocorp.electrocorpplatform.acceptance;

import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Business "Given" steps for the Billing feature. Authentication and the generic
 * HTTP steps are provided by {@link IamSteps} and {@link CommonHttpSteps}.
 */
public class BillingSteps {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestContext ctx;

    @LocalServerPort
    private int port;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders authJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (ctx.jwtToken != null) {
            headers.setBearerAuth(ctx.jwtToken);
        }
        return headers;
    }

    @Given("the billing plan catalog is initialized")
    public void theBillingPlanCatalogIsInitialized() {
        // BillingPlanCatalogInitializer seeds the plans on application startup,
        // so this is a no-op assertion placeholder for readability.
    }

    @Given("the client has an active subscription with plan {string}")
    public void theClientHasAnActiveSubscriptionWithPlan(String planCode) {
        String body = "{\"planCode\":\"%s\"}".formatted(planCode);
        rest.postForEntity(url("/api/v1/billing/subscriptions"),
                new HttpEntity<>(body, authJsonHeaders()), String.class);
    }

    @Given("the client has an active subscription with id {long}")
    public void theClientHasAnActiveSubscriptionWithId(Long id) {
        theClientHasAnActiveSubscriptionWithPlan("ENTERPRISE");
    }

    @Given("the client has at least one paid invoice")
    public void theClientHasAtLeastOnePaidInvoice() {
        theClientHasAnActiveSubscriptionWithPlan("ENTERPRISE");
        // A checkout/payment generates the invoice; extend here if your flow differs.
    }
}
