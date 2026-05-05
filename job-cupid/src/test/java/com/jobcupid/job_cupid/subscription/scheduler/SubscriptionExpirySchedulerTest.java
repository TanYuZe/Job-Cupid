package com.jobcupid.job_cupid.subscription.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobcupid.job_cupid.subscription.entity.Subscription;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionPlan;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionStatus;
import com.jobcupid.job_cupid.subscription.repository.SubscriptionRepository;
import com.jobcupid.job_cupid.subscription.service.SubscriptionService;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpirySchedulerTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock SubscriptionService     subscriptionService;

    @InjectMocks SubscriptionExpiryScheduler scheduler;

    private Subscription expiredSub(UUID userId) {
        return Subscription.builder()
                .id(UUID.randomUUID()).userId(userId)
                .plan(SubscriptionPlan.PREMIUM_MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(OffsetDateTime.now().minusDays(31))
                .currentPeriodEnd(OffsetDateTime.now().minusHours(1))
                .build();
    }

    @Test
    void runExpirySync_marksAllExpiredSubscriptionsAndSyncsPremiumFlag_whenThreeExpired() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();

        List<Subscription> subs = List.of(
                expiredSub(userId1), expiredSub(userId2), expiredSub(userId3));

        when(subscriptionRepository.findByStatusAndCurrentPeriodEndBefore(
                eq(SubscriptionStatus.ACTIVE), any(OffsetDateTime.class)))
                .thenReturn(subs);
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        scheduler.runExpirySync();

        subs.forEach(sub -> {
            verify(subscriptionRepository).save(sub);
            verify(subscriptionService).syncPremiumFlag(sub.getUserId(), false);
        });
        verify(subscriptionService, times(3)).syncPremiumFlag(any(UUID.class), eq(false));
    }

    @Test
    void runExpirySync_makesNoChanges_whenNoExpiredSubscriptions() {
        when(subscriptionRepository.findByStatusAndCurrentPeriodEndBefore(
                eq(SubscriptionStatus.ACTIVE), any(OffsetDateTime.class)))
                .thenReturn(List.of());

        scheduler.runExpirySync();

        verify(subscriptionRepository, never()).save(any());
        verify(subscriptionService, never()).syncPremiumFlag(any(), any(Boolean.class));
    }
}
