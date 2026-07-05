package com.electrocorp.electrocorpplatform.billing.application.gateways;

public record PaymentGatewayChargeResult(
        boolean approved,
        String provider,
        String transactionId,
        String statusMessage
) {
}
