package com.electrocorp.electrocorpplatform.billing.domain.model.aggregates;

import com.electrocorp.electrocorpplatform.billing.domain.model.*;

import com.electrocorp.electrocorpplatform.billing.domain.model.commands.SubscribeCommand;
import com.electrocorp.electrocorpplatform.shared.domain.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscriptions")
public class Subscription extends AuditableEntity {

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionStatus status;

    private LocalDate startDate;
    private LocalDate endDate;

    public Subscription(SubscribeCommand command, Plan plan) {
        if (command.userId() == null) {
            throw new IllegalArgumentException("Subscription user is required.");
        }
        if (plan == null) {
            throw new IllegalArgumentException("Subscription plan is required.");
        }

        this.userId = command.userId();
        this.plan = plan;
        this.status = SubscriptionStatus.ACTIVE;
        this.startDate = LocalDate.now();
        this.endDate = null;
    }

    public boolean isActive() {
        if (status == SubscriptionStatus.ACTIVE) {
            return true;
        }

        return status == SubscriptionStatus.CANCELLED
                && endDate != null
                && !endDate.isBefore(LocalDate.now());
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
        this.endDate = currentBillingCycleEndDate(LocalDate.now());
    }

    private LocalDate currentBillingCycleEndDate(LocalDate today) {
        if (startDate == null) {
            return today;
        }

        LocalDate nextBillingDate = startDate.plusMonths(1);
        while (!nextBillingDate.isAfter(today)) {
            nextBillingDate = nextBillingDate.plusMonths(1);
        }

        return nextBillingDate.minusDays(1);
    }
}
