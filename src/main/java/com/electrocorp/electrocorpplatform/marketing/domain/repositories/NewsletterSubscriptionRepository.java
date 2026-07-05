package com.electrocorp.electrocorpplatform.marketing.domain.repositories;

import com.electrocorp.electrocorpplatform.marketing.domain.model.aggregates.NewsletterSubscription;

import java.util.Optional;

public interface NewsletterSubscriptionRepository {
    Optional<NewsletterSubscription> findByEmail(String email);

    NewsletterSubscription save(NewsletterSubscription subscription);
}
