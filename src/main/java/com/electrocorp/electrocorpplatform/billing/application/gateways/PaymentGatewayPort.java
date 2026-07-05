package com.electrocorp.electrocorpplatform.billing.application.gateways;

public interface PaymentGatewayPort {
    PaymentGatewayChargeResult charge(PaymentGatewayChargeRequest request);
}
