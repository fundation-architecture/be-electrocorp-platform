package com.electrocorp.electrocorpplatform.billing.application.gateways;

import com.electrocorp.electrocorpplatform.shared.domain.valueobjects.Money;

public record PaymentGatewayChargeRequest(
        Long userId,
        String planCode,
        Money amount,
        String holderName,
        String cardNumber,
        String expirationDate
) {
}
