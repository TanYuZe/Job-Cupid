package com.jobcupid.job_cupid.subscription.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobcupid.job_cupid.subscription.entity.Subscription;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionStatus;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findTopByUserIdAndStatusOrderByCurrentPeriodEndDesc(
            UUID userId, SubscriptionStatus status);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    List<Subscription> findByStatusAndCurrentPeriodEndBefore(
            SubscriptionStatus status, OffsetDateTime cutoff);
}
