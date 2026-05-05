package com.jobcupid.job_cupid.swipe.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobcupid.job_cupid.application.entity.Application;
import com.jobcupid.job_cupid.application.repository.ApplicationRepository;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.entity.JobStatus;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.shared.exception.JobNotAvailableException;
import com.jobcupid.job_cupid.shared.exception.PremiumRequiredException;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.subscription.service.SubscriptionService;
import com.jobcupid.job_cupid.shared.service.SwipeLimitService;
import com.jobcupid.job_cupid.swipe.dto.CandidateSummary;
import com.jobcupid.job_cupid.swipe.dto.JobLikersResponse;
import com.jobcupid.job_cupid.swipe.dto.SwipeResponse;
import com.jobcupid.job_cupid.swipe.entity.EmployerSwipeAction;
import com.jobcupid.job_cupid.swipe.entity.SwipeAction;
import com.jobcupid.job_cupid.swipe.event.SwipeEvent;
import com.jobcupid.job_cupid.swipe.event.SwipeEventPublisher;
import com.jobcupid.job_cupid.swipe.repository.CandidateSwipeRepository;
import com.jobcupid.job_cupid.swipe.repository.EmployerSwipeRepository;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SwipeService {

    private final CandidateSwipeRepository candidateSwipeRepository;
    private final EmployerSwipeRepository  employerSwipeRepository;
    private final ApplicationRepository    applicationRepository;
    private final JobRepository            jobRepository;
    private final UserRepository           userRepository;
    private final SwipeEventPublisher      swipeEventPublisher;
    private final SwipeLimitService        swipeLimitService;
    private final SubscriptionService      subscriptionService;

    @Transactional
    public SwipeResponse candidateSwipe(UUID candidateId, UUID jobId, SwipeAction action) {
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + candidateId));

        Job job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.ACTIVE) {
            throw new JobNotAvailableException("Job is no longer available");
        }

        boolean isNewSwipe = !candidateSwipeRepository.existsByCandidateIdAndJobId(candidateId, jobId);

        if (isNewSwipe) {
            swipeLimitService.incrementAndCheck(candidateId, Boolean.TRUE.equals(candidate.getIsPremium()));
        }

        candidateSwipeRepository.upsert(candidateId, jobId, action.name());

        swipeEventPublisher.publish(new SwipeEvent(
                UUID.randomUUID().toString(),
                candidateId,
                "CANDIDATE",
                jobId,
                "JOB",
                action.name(),
                Instant.now()
        ));

        return SwipeResponse.builder()
                .action(action.name())
                .recorded(true)
                .build();
    }

    @Transactional
    public SwipeResponse employerSwipe(UUID employerId, UUID applicationId, EmployerSwipeAction action) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        Job job = jobRepository.findByIdAndDeletedAtIsNull(application.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + application.getJobId()));

        if (!job.getEmployerId().equals(employerId)) {
            throw new AccessDeniedException("You do not own this job posting");
        }

        employerSwipeRepository.upsert(
                employerId,
                applicationId,
                application.getCandidateId(),
                application.getJobId(),
                action.name()
        );

        swipeEventPublisher.publish(new SwipeEvent(
                UUID.randomUUID().toString(),
                employerId,
                "EMPLOYER",
                applicationId,
                "APPLICATION",
                action.name(),
                Instant.now()
        ));

        return SwipeResponse.builder()
                .action(action.name())
                .recorded(true)
                .build();
    }

    public JobLikersResponse getJobLikers(UUID employerId, UUID jobId) {
        if (!subscriptionService.isActive(employerId)) {
            throw new PremiumRequiredException();
        }

        Job job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (!job.getEmployerId().equals(employerId)) {
            throw new AccessDeniedException("You do not own this job posting");
        }

        List<CandidateSummary> likers = candidateSwipeRepository
                .findByJobIdAndAction(jobId, SwipeAction.LIKE)
                .stream()
                .map(swipe -> CandidateSummary.builder().candidateId(swipe.getCandidateId()).build())
                .toList();

        return JobLikersResponse.builder()
                .jobId(jobId)
                .likers(likers)
                .build();
    }
}
