package com.electrocorp.electrocorpplatform.billing.interfaces.rest.resources;

import com.electrocorp.electrocorpplatform.billing.domain.model.aggregates.Payment;

import java.math.BigDecimal;

public record PaymentResource(
        Long id,
        Long userId,
        BigDecimal amount,
        String currency,
        String status,
        String paymentMethod,
        String gatewayTransactionId,
        String gatewayStatusMessage
) {
    public static PaymentResource from(Payment payment) {
        return new PaymentResource(
                payment.getId(),
                payment.getUserId(),
                payment.getAmount() != null ? payment.getAmount().getAmount() : null,
                payment.getAmount() != null ? payment.getAmount().getCurrency() : null,
                payment.getStatus().name(),
                payment.getPaymentMethod(),
                payment.getGatewayTransactionId(),
                payment.getGatewayStatusMessage()
        );
    }
}
