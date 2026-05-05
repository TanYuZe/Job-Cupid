package com.jobcupid.job_cupid.subscription.service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.jobcupid.job_cupid.subscription.entity.Subscription;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionPlan;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionStatus;
import com.jobcupid.job_cupid.subscription.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final StripeSignatureVerifier verifier;
    private final SubscriptionRepository  subscriptionRepository;
    private final SubscriptionService     subscriptionService;

    @Transactional
    public void handleEvent(String payload, String sigHeader) throws StripeException {
        Event event = verifier.verify(payload, sigHeader);

        StripeObject stripeObject = event.getDataObjectDeserializer()
                .getObject().orElse(null);

        switch (event.getType()) {
            case "checkout.session.completed" ->
                handleCheckoutCompleted((Session) stripeObject);
            case "customer.subscription.deleted" ->
                handleSubscriptionEnded((com.stripe.model.Subscription) stripeObject,
                        SubscriptionStatus.CANCELLED);
            case "invoice.payment_failed" ->
                handleSubscriptionEnded((com.stripe.model.Subscription) stripeObject,
                        SubscriptionStatus.PAST_DUE);
            default ->
                log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Session session) {
        if (session == null) return;

        String stripeSubscriptionId = session.getSubscription();
        if (subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId).isPresent()) {
            log.debug("Subscription {} already processed — skipping duplicate checkout event",
                    stripeSubscriptionId);
            return;
        }

        Map<String, String> metadata = session.getMetadata();
        UUID userId = UUID.fromString(metadata.get("userId"));
        SubscriptionPlan plan = SubscriptionPlan.valueOf(metadata.get("plan"));

        OffsetDateTime now   = OffsetDateTime.now();
        OffsetDateTime end   = plan == SubscriptionPlan.PREMIUM_ANNUAL
                ? now.plusDays(365) : now.plusDays(30);

        subscriptionRepository.save(Subscription.builder()
                .userId(userId)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .stripeCustomerId(session.getCustomer())
                .stripeSubscriptionId(stripeSubscriptionId)
                .currentPeriodStart(now)
                .currentPeriodEnd(end)
                .build());

        subscriptionService.syncPremiumFlag(userId, true);
        log.info("Subscription activated for userId={}", userId);
    }

    private void handleSubscriptionEnded(com.stripe.model.Subscription stripeSub,
                                         SubscriptionStatus newStatus) {
        if (stripeSub == null) return;

        Optional<Subscription> existing = subscriptionRepository
                .findByStripeSubscriptionId(stripeSub.getId());

        existing.ifPresentOrElse(sub -> {
            sub.setStatus(newStatus);
            if (newStatus == SubscriptionStatus.CANCELLED) {
                sub.setCancelledAt(OffsetDateTime.now());
            }
            subscriptionRepository.save(sub);
            subscriptionService.syncPremiumFlag(sub.getUserId(), false);
            log.info("Subscription {} → {} for userId={}", stripeSub.getId(), newStatus, sub.getUserId());
        }, () -> log.warn("Received {} event for unknown stripeSubscriptionId={}",
                newStatus, stripeSub.getId()));
    }
}
