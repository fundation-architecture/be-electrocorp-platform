package com.electrocorp.electrocorpplatform.billing.interfaces.rest.controllers;

import com.electrocorp.electrocorpplatform.billing.application.commandservices.BillingCommandService;
import com.electrocorp.electrocorpplatform.billing.application.queryservices.BillingQueryService;
import com.electrocorp.electrocorpplatform.billing.domain.model.commands.CancelSubscriptionCommand;
import com.electrocorp.electrocorpplatform.billing.domain.model.queries.GetAllPlansQuery;
import com.electrocorp.electrocorpplatform.billing.domain.model.queries.GetCurrentSubscriptionQuery;
import com.electrocorp.electrocorpplatform.billing.domain.model.queries.GetInvoicesByUserQuery;
import com.electrocorp.electrocorpplatform.billing.domain.model.queries.GetPaymentsByUserQuery;
import com.electrocorp.electrocorpplatform.billing.interfaces.rest.resources.*;
import com.electrocorp.electrocorpplatform.billing.interfaces.rest.transform.InvoiceResourceFromEntityAssembler;
import com.electrocorp.electrocorpplatform.billing.interfaces.rest.transform.PaymentResourceFromEntityAssembler;
import com.electrocorp.electrocorpplatform.billing.interfaces.rest.transform.PlanResourceFromEntityAssembler;
import com.electrocorp.electrocorpplatform.billing.interfaces.rest.transform.CheckoutSubscriptionCommandFromResourceAssembler;
import com.electrocorp.electrocorpplatform.billing.interfaces.rest.transform.ProcessPaymentCommandFromResourceAssembler;
import com.electrocorp.electrocorpplatform.billing.interfaces.rest.transform.SubscriptionResourceFromEntityAssembler;
import com.electrocorp.electrocorpplatform.iam.application.security.AccessAuthorizationService;
import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingCommandService billingCommandService;
    private final BillingQueryService billingQueryService;
    private final AccessAuthorizationService accessAuthorizationService;

    @GetMapping("/plans")
    public List<PlanResource> getPlans() {
        return billingQueryService.handle(new GetAllPlansQuery())
                .stream()
                .map(PlanResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @GetMapping("/subscriptions/current")
    public SubscriptionResource getCurrentSubscription() {
        return billingQueryService.handle(new GetCurrentSubscriptionQuery(accessAuthorizationService.requireActiveUser()))
                .map(SubscriptionResourceFromEntityAssembler::toResourceFromEntity)
                .orElse(null);
    }

    @PostMapping("/subscriptions")
    public SubscriptionResource subscribe(@Valid @RequestBody SubscribeResource request) {
        requireBillingAccess();
        throw new IllegalArgumentException("Direct subscription creation is no longer supported. Use /api/v1/billing/subscriptions/checkout.");
    }

    @PostMapping("/subscriptions/checkout")
    public SubscriptionResource checkoutSubscription(@Valid @RequestBody CheckoutSubscriptionResource request) {
        Long userId = requireBillingAccess();
        return SubscriptionResourceFromEntityAssembler.toResourceFromEntity(
                billingCommandService.handle(
                        CheckoutSubscriptionCommandFromResourceAssembler.toCommandFromResource(
                                request,
                                userId
                        )
                )
        );
    }

    @DeleteMapping("/subscriptions/current")
    public void cancelSubscription() {
        billingCommandService.handle(new CancelSubscriptionCommand(requireBillingAccess()));
    }

    @PostMapping("/payments")
    public PaymentResource processPayment(@Valid @RequestBody ProcessPaymentResource request) {
        Long userId = requireBillingAccess();
        return PaymentResourceFromEntityAssembler.toResourceFromEntity(
                billingCommandService.handle(
                        ProcessPaymentCommandFromResourceAssembler.toCommandFromResource(
                                request,
                                userId
                        )
                )
        );
    }

    @GetMapping("/payments")
    public List<PaymentResource> getPayments() {
        return billingQueryService.handle(new GetPaymentsByUserQuery(requireBillingAccess()))
                .stream()
                .map(PaymentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @GetMapping("/invoices")
    public List<InvoiceResource> getInvoices() {
        return billingQueryService.handle(new GetInvoicesByUserQuery(requireBillingAccess()))
                .stream()
                .map(InvoiceResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    private Long requireBillingAccess() {
        return accessAuthorizationService.requirePermission(AccessPermission.MANAGE_BILLING);
    }
}
