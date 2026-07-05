package com.electrocorp.electrocorpplatform.billing.application.internal.commandservices;

import com.electrocorp.electrocorpplatform.billing.application.gateways.PaymentGatewayChargeRequest;
import com.electrocorp.electrocorpplatform.billing.application.gateways.PaymentGatewayChargeResult;
import com.electrocorp.electrocorpplatform.billing.application.gateways.PaymentGatewayPort;
import com.electrocorp.electrocorpplatform.billing.domain.model.SubscriptionStatus;
import com.electrocorp.electrocorpplatform.billing.domain.model.aggregates.Invoice;
import com.electrocorp.electrocorpplatform.billing.domain.model.aggregates.Payment;
import com.electrocorp.electrocorpplatform.billing.domain.model.aggregates.Plan;
import com.electrocorp.electrocorpplatform.billing.domain.model.aggregates.Subscription;
import com.electrocorp.electrocorpplatform.billing.domain.model.commands.CancelSubscriptionCommand;
import com.electrocorp.electrocorpplatform.billing.domain.model.commands.CheckoutSubscriptionCommand;
import com.electrocorp.electrocorpplatform.billing.domain.model.events.PaymentRegisteredEvent;
import com.electrocorp.electrocorpplatform.billing.domain.model.events.SubscriptionActivatedEvent;
import com.electrocorp.electrocorpplatform.billing.domain.repositories.InvoiceRepository;
import com.electrocorp.electrocorpplatform.billing.domain.repositories.PaymentRepository;
import com.electrocorp.electrocorpplatform.billing.domain.repositories.PlanRepository;
import com.electrocorp.electrocorpplatform.billing.domain.repositories.SubscriptionRepository;
import com.electrocorp.electrocorpplatform.billing.domain.services.PaymentValidationService;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import com.electrocorp.electrocorpplatform.shared.domain.valueobjects.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingCommandServiceImplTest {

    @Test
    void checkoutPublishesSubscriptionAndPaymentDomainEvents() {
        PlanRepository planRepository = mock(PlanRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        PaymentGatewayPort paymentGatewayPort = mock(PaymentGatewayPort.class);
        DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);

        Plan plan = new Plan();
        plan.setCode("PRO");
        plan.setMonthlyPrice(Money.of(BigDecimal.valueOf(49)));

        when(planRepository.findByCode("PRO")).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findFirstByUserIdAndStatus(eq(7L), eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGatewayPort.charge(any(PaymentGatewayChargeRequest.class)))
                .thenReturn(new PaymentGatewayChargeResult(true, "STRIPE_DEMO", "pi_demo_7_1111", "Approved."));

        BillingCommandServiceImpl commandService = new BillingCommandServiceImpl(
                planRepository,
                subscriptionRepository,
                paymentRepository,
                invoiceRepository,
                new PaymentValidationService(),
                paymentGatewayPort,
                domainEventPublisher
        );

        commandService.handle(new CheckoutSubscriptionCommand(
                7L,
                "PRO",
                "Ada Lovelace",
                "4111111111111111",
                "12/30",
                "123"
        ));

        verify(paymentGatewayPort).charge(isA(PaymentGatewayChargeRequest.class));
        verify(domainEventPublisher).publish(isA(SubscriptionActivatedEvent.class));
        verify(domainEventPublisher).publish(isA(PaymentRegisteredEvent.class));
    }

    @Test
    void cancelSubscriptionKeepsEndDateAtCurrentBillingCycleEnd() {
        PlanRepository planRepository = mock(PlanRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        PaymentGatewayPort paymentGatewayPort = mock(PaymentGatewayPort.class);
        DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
        Subscription subscription = new Subscription();
        subscription.setUserId(7L);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now().minusDays(10));

        when(subscriptionRepository.findFirstByUserIdAndStatus(eq(7L), eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillingCommandServiceImpl commandService = new BillingCommandServiceImpl(
                planRepository,
                subscriptionRepository,
                paymentRepository,
                invoiceRepository,
                new PaymentValidationService(),
                paymentGatewayPort,
                domainEventPublisher
        );

        commandService.handle(new CancelSubscriptionCommand(7L));

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(subscription.getEndDate()).isAfterOrEqualTo(LocalDate.now());
        assertThat(subscription.isActive()).isTrue();
    }
}
