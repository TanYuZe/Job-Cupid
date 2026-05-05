package com.jobcupid.job_cupid.swipe.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.jobcupid.job_cupid.shared.exception.SwipeLimitExceededException;
import com.jobcupid.job_cupid.shared.service.SwipeLimitService;

@ExtendWith(MockitoExtension.class)
class SwipeLimitServiceTest {

    @Mock StringRedisTemplate              redisTemplate;
    @Mock ValueOperations<String, String>  valueOps;

    @InjectMocks SwipeLimitService swipeLimitService;

    @Test
    void incrementAndCheck_doesNotThrow_whenFreeUserAt19Swipes() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(20L); // 19 → 20, still within limit

        assertThatNoException().isThrownBy(
                () -> swipeLimitService.incrementAndCheck(UUID.randomUUID(), false));
    }

    @Test
    void incrementAndCheck_throwsSwipeLimitExceededException_whenFreeUserAt20Swipes() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(21L); // 20 → 21, over limit

        assertThatThrownBy(() -> swipeLimitService.incrementAndCheck(UUID.randomUUID(), false))
                .isInstanceOf(SwipeLimitExceededException.class);
    }

    @Test
    void incrementAndCheck_doesNotThrow_whenPremiumUserAt100Swipes() {
        assertThatNoException().isThrownBy(
                () -> swipeLimitService.incrementAndCheck(UUID.randomUUID(), true));

        verify(redisTemplate, never()).opsForValue();
    }
}
