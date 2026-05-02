package com.jobcupid.job_cupid.swipe.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.shared.exception.SwipeLimitExceededException;
import com.jobcupid.job_cupid.swipe.dto.SwipeResponse;
import com.jobcupid.job_cupid.swipe.repository.CandidateSwipeRepository;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SwipeService {

    private static final int FREE_DAILY_LIMIT = 20;
    private static final String SWIPE_COUNT_KEY = "swipe:daily:%s:%s";

    private final CandidateSwipeRepository candidateSwipeRepository;
    private final JobRepository            jobRepository;
    private final UserRepository           userRepository;
    private final StringRedisTemplate      redisTemplate;

    @Transactional
    public SwipeResponse swipeJob(UUID candidateId, UUID jobId, String action) {
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + candidateId));

        jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        boolean isNewSwipe = !candidateSwipeRepository.existsByCandidateIdAndJobId(candidateId, jobId);

        if (isNewSwipe && !Boolean.TRUE.equals(candidate.getIsPremium())) {
            enforceRateLimit(candidateId);
        }

        candidateSwipeRepository.upsert(candidateId, jobId, action);

        if (isNewSwipe) {
            incrementDailyCount(candidateId);
        }

        return SwipeResponse.builder()
                .action(action)
                .recorded(true)
                .build();
    }

    private void enforceRateLimit(UUID candidateId) {
        String key = String.format(SWIPE_COUNT_KEY, candidateId, LocalDate.now());
        String raw = redisTemplate.opsForValue().get(key);
        int count = raw == null ? 0 : Integer.parseInt(raw);
        if (count >= FREE_DAILY_LIMIT) {
            throw new SwipeLimitExceededException();
        }
    }

    private void incrementDailyCount(UUID candidateId) {
        String key = String.format(SWIPE_COUNT_KEY, candidateId, LocalDate.now());
        Long newCount = redisTemplate.opsForValue().increment(key);
        if (newCount != null && newCount == 1L) {
            redisTemplate.expire(key, Duration.ofHours(25));
        }
    }
}
