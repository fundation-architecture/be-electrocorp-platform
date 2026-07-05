package com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources;

import com.electrocorp.electrocorpplatform.marketing.domain.model.aggregates.NewsletterSubscription;

import java.time.LocalDateTime;

public record NewsletterSubscriptionResource(
        Long id,
        String email,
        String source,
        Boolean active,
        LocalDateTime subscribedAt
) {
    public static NewsletterSubscriptionResource from(NewsletterSubscription subscription) {
        return new NewsletterSubscriptionResource(
                subscription.getId(),
                subscription.getEmail(),
                subscription.getSource(),
                subscription.getActive(),
                subscription.getSubscribedAt()
        );
    }
}
