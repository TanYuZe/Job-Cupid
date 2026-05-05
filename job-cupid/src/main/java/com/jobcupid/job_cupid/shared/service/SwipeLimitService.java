package com.jobcupid.job_cupid.shared.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.jobcupid.job_cupid.shared.exception.SwipeLimitExceededException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SwipeLimitService {

    static final int    FREE_DAILY_LIMIT = 20;
    static final String KEY_PREFIX       = "swipe:limit:";

    private final StringRedisTemplate redisTemplate;

    public void incrementAndCheck(UUID userId, boolean isPremium) {
        if (isPremium) return;

        String key   = KEY_PREFIX + userId + ":" + LocalDate.now(ZoneOffset.UTC);
        Long   count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1L) {
            redisTemplate.expireAt(key,
                    LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        }

        if (count != null && count > FREE_DAILY_LIMIT) {
            throw new SwipeLimitExceededException();
        }
    }
}
