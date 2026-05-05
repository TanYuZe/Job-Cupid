package com.jobcupid.job_cupid.subscription.controller;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.jobcupid.job_cupid.subscription.dto.CheckoutRequest;
import com.jobcupid.job_cupid.subscription.dto.CheckoutResponse;
import com.jobcupid.job_cupid.subscription.dto.SubscriptionStatusResponse;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionPlan;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionStatus;
import com.jobcupid.job_cupid.subscription.repository.SubscriptionRepository;
import com.jobcupid.job_cupid.subscription.service.StripeCheckoutService;
import com.jobcupid.job_cupid.subscription.service.StripeWebhookService;
import com.jobcupid.job_cupid.shared.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    private final StripeCheckoutService  stripeCheckoutService;
    private final StripeWebhookService   stripeWebhookService;
    private final SubscriptionRepository subscriptionRepository;

    @PostMapping("/checkout")
    @Secured({"ROLE_USER", "ROLE_EMPLOYER"})
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CheckoutRequest request) throws StripeException {
        String url = stripeCheckoutService.createCheckoutUrl(principal.getId(), request.getPlan());
        return ResponseEntity.ok(CheckoutResponse.builder().checkoutUrl(url).build());
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            stripeWebhookService.handleEvent(payload, sigHeader);
            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (StripeException e) {
            log.error("Stripe webhook processing error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/me")
    @Secured({"ROLE_USER", "ROLE_EMPLOYER"})
    public ResponseEntity<SubscriptionStatusResponse> getMySubscription(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getId();

        return subscriptionRepository
                .findTopByUserIdAndStatusOrderByCurrentPeriodEndDesc(userId, SubscriptionStatus.ACTIVE)
                .map(sub -> SubscriptionStatusResponse.builder()
                        .active(sub.getCurrentPeriodEnd().isAfter(OffsetDateTime.now()))
                        .plan(sub.getPlan().name())
                        .status(sub.getStatus().name())
                        .currentPeriodEnd(sub.getCurrentPeriodEnd().toInstant())
                        .build())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(SubscriptionStatusResponse.builder()
                        .active(false).build()));
    }

    @GetMapping("/plans")
    public ResponseEntity<List<String>> getPlans() {
        return ResponseEntity.ok(Arrays.stream(SubscriptionPlan.values())
                .map(SubscriptionPlan::name)
                .toList());
    }
}
