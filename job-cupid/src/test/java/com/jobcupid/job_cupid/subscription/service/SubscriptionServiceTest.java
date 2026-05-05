package com.jobcupid.job_cupid.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.jobcupid.job_cupid.subscription.entity.Subscription;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionPlan;
import com.jobcupid.job_cupid.subscription.entity.SubscriptionStatus;
import com.jobcupid.job_cupid.subscription.repository.SubscriptionRepository;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;
import com.jobcupid.job_cupid.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock SubscriptionRepository        subscriptionRepository;
    @Mock UserRepository                userRepository;
    @Mock StringRedisTemplate           redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    @InjectMocks SubscriptionService subscriptionService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    private Subscription activeSubscription(OffsetDateTime periodEnd) {
        return Subscription.builder()
                .id(UUID.randomUUID()).userId(userId)
                .plan(SubscriptionPlan.PREMIUM_MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(OffsetDateTime.now().minusDays(30))
                .currentPeriodEnd(periodEnd)
                .build();
    }

    // ── isActive ──────────────────────────────────────────────────────────────

    @Test
    void isActive_returnsTrue_whenActiveSubscriptionExistsWithFuturePeriodEnd() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(subscriptionRepository.findTopByUserIdAndStatusOrderByCurrentPeriodEndDesc(
                userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSubscription(OffsetDateTime.now().plusDays(10))));

        boolean result = subscriptionService.isActive(userId);

        assertThat(result).isTrue();
        verify(valueOperations).set(anyString(), eq("true"), any());
    }

    @Test
    void isActive_returnsFalse_whenSubscriptionExpired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(subscriptionRepository.findTopByUserIdAndStatusOrderByCurrentPeriodEndDesc(
                userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSubscription(OffsetDateTime.now().minusDays(1))));

        boolean result = subscriptionService.isActive(userId);

        assertThat(result).isFalse();
        verify(valueOperations).set(anyString(), eq("false"), any());
    }

    @Test
    void isActive_returnsFalse_whenNoSubscriptionExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(subscriptionRepository.findTopByUserIdAndStatusOrderByCurrentPeriodEndDesc(
                userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        boolean result = subscriptionService.isActive(userId);

        assertThat(result).isFalse();
    }

    @Test
    void isActive_hitsRepositoryOnlyOnce_whenCalledTwiceWithCachedResult() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn(null)
                .thenReturn("true");
        when(subscriptionRepository.findTopByUserIdAndStatusOrderByCurrentPeriodEndDesc(
                userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSubscription(OffsetDateTime.now().plusDays(10))));

        subscriptionService.isActive(userId);
        boolean second = subscriptionService.isActive(userId);

        assertThat(second).isTrue();
        verify(subscriptionRepository, times(1))
                .findTopByUserIdAndStatusOrderByCurrentPeriodEndDesc(userId, SubscriptionStatus.ACTIVE);
    }

    // ── syncPremiumFlag ───────────────────────────────────────────────────────

    @Test
    void syncPremiumFlag_setsIsPremiumTrueAndInvalidatesCache_whenSubscriptionActivated() {
        User user = User.builder()
                .id(userId).email("alice@example.com")
                .passwordHash("x").firstName("Alice").lastName("Chen")
                .role(UserRole.USER).isPremium(false).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        subscriptionService.syncPremiumFlag(userId, true);

        assertThat(user.getIsPremium()).isTrue();
        verify(userRepository).save(user);
        verify(redisTemplate).delete("sub:active:" + userId);
    }

    @Test
    void syncPremiumFlag_setsIsPremiumFalseAndInvalidatesCache_whenSubscriptionCancelled() {
        User user = User.builder()
                .id(userId).email("alice@example.com")
                .passwordHash("x").firstName("Alice").lastName("Chen")
                .role(UserRole.USER).isPremium(true).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        subscriptionService.syncPremiumFlag(userId, false);

        assertThat(user.getIsPremium()).isFalse();
        verify(redisTemplate).delete("sub:active:" + userId);
    }

    @Test
    void syncPremiumFlag_doesNotSave_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        subscriptionService.syncPremiumFlag(userId, true);

        verify(userRepository, never()).save(any());
        verify(redisTemplate).delete("sub:active:" + userId);
    }
}
