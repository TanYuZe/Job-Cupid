package com.jobcupid.job_cupid.subscription.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.jobcupid.job_cupid.subscription.entity.Subscription;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionStatus;
import com.jobcupid.job_cupid.subscription.repository.SubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock StripeSignatureVerifier verifier;
    @Mock SubscriptionRepository  subscriptionRepository;
    @Mock SubscriptionService     subscriptionService;

    @InjectMocks StripeWebhookService stripeWebhookService;

    private UUID   userId;
    private String stripeSubscriptionId;
    private String stripeCustomerId;

    @BeforeEach
    void setUp() {
        userId               = UUID.randomUUID();
        stripeSubscriptionId = "sub_test_" + UUID.randomUUID();
        stripeCustomerId     = "cus_test_" + UUID.randomUUID();
    }

    // ── checkout.session.completed ────────────────────────────────────────────

    @Test
    void handleEvent_createsSubscriptionAndActivatesPremium_whenCheckoutCompleted()
            throws StripeException {
        Event                      event        = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        Session                    session      = mock(Session.class);

        when(verifier.verify(any(), any())).thenReturn(event);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));
        when(session.getSubscription()).thenReturn(stripeSubscriptionId);
        when(session.getCustomer()).thenReturn(stripeCustomerId);
        when(session.getMetadata()).thenReturn(
                Map.of("userId", userId.toString(), "plan", "PREMIUM_MONTHLY"));
        when(subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        stripeWebhookService.handleEvent("payload", "sig");

        verify(subscriptionRepository).save(any(Subscription.class));
        verify(subscriptionService).syncPremiumFlag(userId, true);
    }

    @Test
    void handleEvent_isIdempotent_whenCheckoutEventProcessedTwice() throws StripeException {
        Event                      event        = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        Session                    session      = mock(Session.class);
        Subscription               existing     = Subscription.builder()
                .id(UUID.randomUUID()).userId(userId)
                .stripeSubscriptionId(stripeSubscriptionId).build();

        when(verifier.verify(any(), any())).thenReturn(event);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));
        when(session.getSubscription()).thenReturn(stripeSubscriptionId);
        when(subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId))
                .thenReturn(Optional.of(existing));

        stripeWebhookService.handleEvent("payload", "sig");

        verify(subscriptionRepository, never()).save(any());
        verify(subscriptionService, never()).syncPremiumFlag(any(), eq(true));
    }

    // ── customer.subscription.deleted ─────────────────────────────────────────

    @Test
    void handleEvent_cancelsSubscriptionAndDeactivatesPremium_whenSubscriptionDeleted()
            throws StripeException {
        Event                             event        = mock(Event.class);
        EventDataObjectDeserializer        deserializer = mock(EventDataObjectDeserializer.class);
        com.stripe.model.Subscription     stripeSub    = mock(com.stripe.model.Subscription.class);
        Subscription                      localSub     = Subscription.builder()
                .id(UUID.randomUUID()).userId(userId)
                .stripeSubscriptionId(stripeSubscriptionId)
                .status(SubscriptionStatus.ACTIVE).build();

        when(verifier.verify(any(), any())).thenReturn(event);
        when(event.getType()).thenReturn("customer.subscription.deleted");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(stripeSub));
        when(stripeSub.getId()).thenReturn(stripeSubscriptionId);
        when(subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId))
                .thenReturn(Optional.of(localSub));
        when(subscriptionRepository.save(localSub)).thenReturn(localSub);

        stripeWebhookService.handleEvent("payload", "sig");

        verify(subscriptionRepository).save(localSub);
        verify(subscriptionService).syncPremiumFlag(userId, false);
    }

    // ── invalid signature ─────────────────────────────────────────────────────

    @Test
    void handleEvent_throwsSignatureVerificationException_whenSignatureInvalid()
            throws StripeException {
        when(verifier.verify(any(), any()))
                .thenThrow(new SignatureVerificationException("Invalid signature", "bad_sig"));

        assertThatThrownBy(() -> stripeWebhookService.handleEvent("payload", "bad_sig"))
                .isInstanceOf(SignatureVerificationException.class);

        verify(subscriptionRepository, never()).save(any());
        verify(subscriptionService, never()).syncPremiumFlag(any(), any(Boolean.class));
    }
}
