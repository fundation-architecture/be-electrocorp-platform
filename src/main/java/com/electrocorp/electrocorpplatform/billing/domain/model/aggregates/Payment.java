package com.electrocorp.electrocorpplatform.billing.domain.model.aggregates;

import com.electrocorp.electrocorpplatform.billing.domain.model.*;

import com.electrocorp.electrocorpplatform.billing.domain.model.commands.ProcessPaymentCommand;
import com.electrocorp.electrocorpplatform.shared.domain.model.AuditableEntity;
import com.electrocorp.electrocorpplatform.shared.domain.valueobjects.Money;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends AuditableEntity {

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "currency", length = 10))
    })
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(length = 50)
    private String paymentMethod;

    @Column(length = 120)
    private String gatewayTransactionId;

    @Column(length = 240)
    private String gatewayStatusMessage;

    private LocalDateTime paidAt;

    public Payment(ProcessPaymentCommand command, Subscription subscription) {
        if (command.userId() == null) {
            throw new IllegalArgumentException("Payment user is required.");
        }
        if (subscription == null) {
            throw new IllegalArgumentException("Payment subscription is required.");
        }
        if (!command.userId().equals(subscription.getUserId())) {
            throw new IllegalArgumentException("Subscription does not belong to the current user.");
        }
        if (!subscription.isActive()) {
            throw new IllegalArgumentException("Subscription is not active.");
        }
        if (subscription.getPlan() == null || subscription.getPlan().getMonthlyPrice() == null) {
            throw new IllegalArgumentException("Subscription plan price is required.");
        }

        this.userId = command.userId();
        this.subscription = subscription;
        this.amount = subscription.getPlan().getMonthlyPrice();
        this.status = PaymentStatus.APPROVED;
        this.paymentMethod = "CARD";
        this.paidAt = LocalDateTime.now();
    }

    public void registerGatewayResult(String provider, String transactionId, String statusMessage) {
        if (provider != null && !provider.isBlank()) {
            this.paymentMethod = provider.trim();
        }
        this.gatewayTransactionId = normalizeNullable(transactionId, 120);
        this.gatewayStatusMessage = normalizeNullable(statusMessage, 240);
    }

    private String normalizeNullable(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
