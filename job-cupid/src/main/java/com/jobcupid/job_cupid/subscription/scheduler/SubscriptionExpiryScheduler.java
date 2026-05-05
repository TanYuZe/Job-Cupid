package com.jobcupid.job_cupid.subscription.scheduler;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jobcupid.job_cupid.subscription.entity.Subscription;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionStatus;
import com.jobcupid.job_cupid.subscription.repository.SubscriptionRepository;
import com.jobcupid.job_cupid.subscription.service.SubscriptionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SubscriptionExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryScheduler.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService    subscriptionService;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void runExpirySync() {
        List<Subscription> expired = subscriptionRepository
                .findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus.ACTIVE, OffsetDateTime.now());

        for (Subscription sub : expired) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            subscriptionService.syncPremiumFlag(sub.getUserId(), false);
            log.info("Subscription {} expired for userId={}", sub.getId(), sub.getUserId());
        }

        log.info("Expiry sync complete: {} subscription(s) marked EXPIRED", expired.size());
    }
}
