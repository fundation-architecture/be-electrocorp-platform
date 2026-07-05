package com.electrocorp.electrocorpplatform.billing.application.internal.commandservices;

import com.electrocorp.electrocorpplatform.billing.application.commandservices.BillingCommandService;
import com.electrocorp.electrocorpplatform.billing.application.gateways.PaymentGatewayChargeRequest;
import com.electrocorp.electrocorpplatform.billing.application.gateways.PaymentGatewayPort;
import com.electrocorp.electrocorpplatform.billing.domain.model.aggregates.Invoice;
import com.electrocorp.electrocorpplatform.billing.domain.model.aggregates.Payment;
import com.electrocorp.electrocorpplatform.billing.domain.model.aggregates.Plan;
import com.electrocorp.electrocorpplatform.billing.domain.model.aggregates.Subscription;
import com.electrocorp.electrocorpplatform.billing.domain.model.SubscriptionStatus;
import com.electrocorp.electrocorpplatform.billing.domain.model.commands.CancelSubscriptionCommand;
import com.electrocorp.electrocorpplatform.billing.domain.model.commands.CheckoutSubscriptionCommand;
import com.electrocorp.electrocorpplatform.billing.domain.model.commands.ProcessPaymentCommand;
import com.electrocorp.electrocorpplatform.billing.domain.model.commands.SubscribeCommand;
import com.electrocorp.electrocorpplatform.billing.domain.model.events.PaymentRegisteredEvent;
import com.electrocorp.electrocorpplatform.billing.domain.model.events.SubscriptionActivatedEvent;
import com.electrocorp.electrocorpplatform.billing.domain.repositories.InvoiceRepository;
import com.electrocorp.electrocorpplatform.billing.domain.repositories.PaymentRepository;
import com.electrocorp.electrocorpplatform.billing.domain.repositories.PlanRepository;
import com.electrocorp.electrocorpplatform.billing.domain.repositories.SubscriptionRepository;
import com.electrocorp.electrocorpplatform.billing.domain.services.PaymentValidationService;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingCommandServiceImpl implements BillingCommandService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentValidationService paymentValidationService;
    private final PaymentGatewayPort paymentGatewayPort;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional
    public Subscription handle(CheckoutSubscriptionCommand command) {
        validatePaymentDetails(command);

        Plan plan = planRepository.findByCode(command.planCode())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found."));

        var chargeResult = paymentGatewayPort.charge(new PaymentGatewayChargeRequest(
                command.userId(),
                plan.getCode(),
                plan.getMonthlyPrice(),
                command.holderName(),
                command.cardNumber(),
                command.expirationDate()
        ));

        if (!chargeResult.approved()) {
            throw new IllegalArgumentException("Payment gateway rejected the charge.");
        }

        subscriptionRepository
                .findFirstByUserIdAndStatus(command.userId(), SubscriptionStatus.ACTIVE)
                .ifPresent(subscription -> {
                    subscription.cancel();
                    subscriptionRepository.save(subscription);
                });

        Subscription subscription = subscriptionRepository.save(
                new Subscription(new SubscribeCommand(command.userId(), command.planCode()), plan)
        );

        Payment payment = paymentRepository.save(
                new Payment(new ProcessPaymentCommand(command.userId(), subscription.getId()), subscription)
        );
        payment.registerGatewayResult(
                chargeResult.provider(),
                chargeResult.transactionId(),
                chargeResult.statusMessage()
        );
        payment = paymentRepository.save(payment);

        invoiceRepository.save(new Invoice(payment));
        domainEventPublisher.publish(new SubscriptionActivatedEvent(
                command.userId(),
                subscription.getId(),
                plan.getCode()
        ));
        domainEventPublisher.publish(new PaymentRegisteredEvent(
                command.userId(),
                payment.getId(),
                payment.getAmount().getAmount()
        ));

        return subscription;
    }

    @Override
    @Transactional
    public void handle(CancelSubscriptionCommand command) {
        Subscription subscription = subscriptionRepository
                .findFirstByUserIdAndStatus(command.userId(), SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Active subscription not found."));

        subscription.cancel();
        subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public Payment handle(ProcessPaymentCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found."));

        Payment payment = new Payment(command, subscription);
        Payment savedPayment = paymentRepository.save(payment);
        invoiceRepository.save(new Invoice(savedPayment));
        domainEventPublisher.publish(new PaymentRegisteredEvent(
                command.userId(),
                savedPayment.getId(),
                savedPayment.getAmount().getAmount()
        ));

        return savedPayment;
    }

    private void validatePaymentDetails(CheckoutSubscriptionCommand command) {
        if (!paymentValidationService.isValidHolderName(command.holderName())) {
            throw new IllegalArgumentException("Invalid card holder name.");
        }
        if (!paymentValidationService.isValidCardNumber(command.cardNumber())) {
            throw new IllegalArgumentException("Invalid card number.");
        }
        if (!paymentValidationService.isValidExpirationDate(command.expirationDate())) {
            throw new IllegalArgumentException("Invalid card expiration date.");
        }
        if (!paymentValidationService.isValidCvv(command.cvv())) {
            throw new IllegalArgumentException("Invalid card CVV.");
        }
    }
}
