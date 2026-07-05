package com.electrocorp.electrocorpplatform.billing.infrastructure.payments;

import com.electrocorp.electrocorpplatform.billing.application.gateways.PaymentGatewayChargeRequest;
import com.electrocorp.electrocorpplatform.billing.application.gateways.PaymentGatewayChargeResult;
import com.electrocorp.electrocorpplatform.billing.application.gateways.PaymentGatewayPort;
import com.electrocorp.electrocorpplatform.shared.domain.valueobjects.Money;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StripePaymentGatewayAdapter implements PaymentGatewayPort {

    private static final String PROVIDER = "STRIPE";
    private static final String DEMO_PROVIDER = "STRIPE_DEMO";

    private final RestClient restClient;

    @Value("${electrocorp.integrations.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${electrocorp.integrations.stripe.payment-intents-url:https://api.stripe.com/v1/payment_intents}")
    private String paymentIntentsUrl;

    @Value("${electrocorp.integrations.stripe.payment-method-token:pm_card_visa}")
    private String paymentMethodToken;

    public StripePaymentGatewayAdapter(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public PaymentGatewayChargeResult charge(PaymentGatewayChargeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment gateway charge request is required.");
        }

        if (!hasStripeCredentials()) {
            return approveDemoCharge(request);
        }

        try {
            Map<?, ?> response = restClient.post()
                    .uri(paymentIntentsUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + stripeSecretKey.trim())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(buildPaymentIntentForm(request))
                    .retrieve()
                    .body(Map.class);

            String transactionId = valueFrom(response, "id");
            String status = valueFrom(response, "status");
            boolean approved = isApprovedStatus(status);

            return new PaymentGatewayChargeResult(
                    approved,
                    PROVIDER,
                    transactionId,
                    "Stripe payment intent %s for plan %s.".formatted(status, request.planCode())
            );
        } catch (RestClientException exception) {
            return new PaymentGatewayChargeResult(
                    false,
                    PROVIDER,
                    null,
                    "Stripe charge rejected: " + exception.getMessage()
            );
        }
    }

    private PaymentGatewayChargeResult approveDemoCharge(PaymentGatewayChargeRequest request) {
        String digits = request.cardNumber() == null
                ? ""
                : request.cardNumber().replaceAll("\\D", "");
        String last4 = digits.length() < 4 ? "0000" : digits.substring(digits.length() - 4);
        String transactionId = "pi_demo_%s_%s_%d".formatted(
                request.userId(),
                last4,
                Instant.now().toEpochMilli()
        );

        return new PaymentGatewayChargeResult(
                true,
                DEMO_PROVIDER,
                transactionId,
                "Stripe demo charge approved for plan %s.".formatted(request.planCode())
        );
    }

    private String buildPaymentIntentForm(PaymentGatewayChargeRequest request) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("amount", String.valueOf(toMinorUnits(request.amount())));
        values.put("currency", currencyFrom(request.amount()));
        values.put("payment_method", paymentMethodToken);
        values.put("confirm", "true");
        values.put("description", "ElectroCorp subscription " + safe(request.planCode()));
        values.put("metadata[user_id]", String.valueOf(request.userId()));
        values.put("metadata[plan_code]", safe(request.planCode()));
        values.put("metadata[holder_name]", safe(request.holderName()));

        return values.entrySet()
                .stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private long toMinorUnits(Money amount) {
        if (amount == null || amount.getAmount() == null) {
            return 0L;
        }

        return amount.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private String currencyFrom(Money amount) {
        if (amount == null || amount.getCurrency() == null || amount.getCurrency().isBlank()) {
            return "pen";
        }

        return amount.getCurrency().trim().toLowerCase();
    }

    private boolean hasStripeCredentials() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank()
                && paymentIntentsUrl != null && !paymentIntentsUrl.isBlank();
    }

    private boolean isApprovedStatus(String status) {
        if (status == null) {
            return false;
        }

        return List.of("succeeded", "processing", "requires_capture")
                .contains(status.trim().toLowerCase());
    }

    private String valueFrom(Map<?, ?> response, String key) {
        if (response == null) {
            return "unknown";
        }

        Object value = response.get(key);
        return value == null ? "unknown" : String.valueOf(value);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
