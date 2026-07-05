package com.electrocorp.electrocorpplatform.marketing.infrastructure.persistence.jpa.repositories;

import com.electrocorp.electrocorpplatform.marketing.domain.model.aggregates.NewsletterSubscription;
import com.electrocorp.electrocorpplatform.marketing.domain.repositories.NewsletterSubscriptionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaNewsletterSubscriptionRepository extends JpaRepository<NewsletterSubscription, Long>, NewsletterSubscriptionRepository {
}
