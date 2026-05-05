package com.jobcupid.job_cupid.subscription.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobcupid.job_cupid.subscription.entity.SubscriptionStatus;
import com.jobcupid.job_cupid.subscription.repository.SubscriptionRepository;
import com.jobcupid.job_cupid.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String   CACHE_KEY_PREFIX = "sub:active:";
    private static final Duration CACHE_TTL        = Duration.ofMinutes(5);

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository         userRepository;
    private final StringRedisTemplate    redisTemplate;

    public boolean isActive(UUID userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;
        String cached   = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return Boolean.parseBoolean(cached);
        }

        boolean active = subscriptionRepository
                .findTopByUserIdAndStatusOrderByCurrentPeriodEndDesc(userId, SubscriptionStatus.ACTIVE)
                .filter(sub -> sub.getCurrentPeriodEnd().isAfter(OffsetDateTime.now()))
                .isPresent();

        redisTemplate.opsForValue().set(cacheKey, String.valueOf(active), CACHE_TTL);
        return active;
    }

    @Transactional
    public void syncPremiumFlag(UUID userId, boolean isPremium) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setIsPremium(isPremium);
            userRepository.save(user);
        });
        redisTemplate.delete(CACHE_KEY_PREFIX + userId);
    }
}
