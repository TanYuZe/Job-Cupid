package com.jobcupid.job_cupid.swipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.jobcupid.job_cupid.application.entity.Application;
import com.jobcupid.job_cupid.application.repository.ApplicationRepository;
import com.jobcupid.job_cupid.job.entity.EmploymentType;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.entity.JobStatus;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.shared.exception.JobNotAvailableException;
import com.jobcupid.job_cupid.shared.exception.PremiumRequiredException;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.subscription.service.SubscriptionService;
import com.jobcupid.job_cupid.shared.service.SwipeLimitService;
import com.jobcupid.job_cupid.swipe.dto.JobLikersResponse;
import com.jobcupid.job_cupid.swipe.dto.SwipeResponse;
import com.jobcupid.job_cupid.swipe.entity.CandidateSwipe;
import com.jobcupid.job_cupid.swipe.entity.EmployerSwipeAction;
import com.jobcupid.job_cupid.swipe.entity.SwipeAction;
import com.jobcupid.job_cupid.swipe.event.SwipeEvent;
import com.jobcupid.job_cupid.swipe.event.SwipeEventPublisher;
import com.jobcupid.job_cupid.swipe.repository.CandidateSwipeRepository;
import com.jobcupid.job_cupid.swipe.repository.EmployerSwipeRepository;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;
import com.jobcupid.job_cupid.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SwipeServiceTest {

    @Mock CandidateSwipeRepository candidateSwipeRepository;
    @Mock EmployerSwipeRepository  employerSwipeRepository;
    @Mock ApplicationRepository    applicationRepository;
    @Mock JobRepository            jobRepository;
    @Mock UserRepository           userRepository;
    @Mock SwipeEventPublisher      swipeEventPublisher;
    @Mock SwipeLimitService        swipeLimitService;
    @Mock SubscriptionService      subscriptionService;

    @InjectMocks SwipeService swipeService;

    private UUID candidateId;
    private UUID employerId;
    private UUID jobId;
    private UUID applicationId;
    private User freeUser;
    private Job  activeJob;

    @BeforeEach
    void setUp() {
        candidateId   = UUID.randomUUID();
        employerId    = UUID.randomUUID();
        jobId         = UUID.randomUUID();
        applicationId = UUID.randomUUID();

        freeUser = User.builder()
                .id(candidateId).email("alice@example.com")
                .passwordHash("x").firstName("Alice").lastName("Chen")
                .role(UserRole.USER).isPremium(false).isActive(true).isBanned(false)
                .build();

        activeJob = Job.builder()
                .id(jobId).employerId(employerId)
                .title("SWE").description("Build").category("Eng")
                .employmentType(EmploymentType.FULL_TIME).status(JobStatus.ACTIVE)
                .build();
    }

    // ── candidateSwipe ────────────────────────────────────────────────────────

    @Test
    void candidateSwipe_savesLikeAndPublishesEvent_forNewSwipeOnActiveJob() {
        when(userRepository.findById(candidateId)).thenReturn(Optional.of(freeUser));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(activeJob));
        when(candidateSwipeRepository.existsByCandidateIdAndJobId(candidateId, jobId)).thenReturn(false);
        doNothing().when(swipeLimitService).incrementAndCheck(candidateId, false);
        doNothing().when(candidateSwipeRepository).upsert(candidateId, jobId, "LIKE");

        SwipeResponse result = swipeService.candidateSwipe(candidateId, jobId, SwipeAction.LIKE);

        assertThat(result.getAction()).isEqualTo("LIKE");
        assertThat(result.isRecorded()).isTrue();
        verify(candidateSwipeRepository).upsert(candidateId, jobId, "LIKE");
        verify(swipeEventPublisher).publish(any(SwipeEvent.class));
    }

    @Test
    void candidateSwipe_updatesActionToPass_whenReSwipingExistingJob() {
        when(userRepository.findById(candidateId)).thenReturn(Optional.of(freeUser));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(activeJob));
        when(candidateSwipeRepository.existsByCandidateIdAndJobId(candidateId, jobId)).thenReturn(true);
        doNothing().when(candidateSwipeRepository).upsert(candidateId, jobId, "PASS");

        SwipeResponse result = swipeService.candidateSwipe(candidateId, jobId, SwipeAction.PASS);

        assertThat(result.getAction()).isEqualTo("PASS");
        verify(swipeLimitService, never()).incrementAndCheck(any(UUID.class), anyBoolean());
        verify(candidateSwipeRepository).upsert(candidateId, jobId, "PASS");
    }

    @Test
    void candidateSwipe_throwsJobNotAvailableException_forClosedJob() {
        Job closedJob = Job.builder()
                .id(jobId).employerId(employerId)
                .title("SWE").description("Build").category("Eng")
                .employmentType(EmploymentType.FULL_TIME).status(JobStatus.CLOSED)
                .build();

        when(userRepository.findById(candidateId)).thenReturn(Optional.of(freeUser));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(closedJob));

        assertThatThrownBy(() -> swipeService.candidateSwipe(candidateId, jobId, SwipeAction.LIKE))
                .isInstanceOf(JobNotAvailableException.class);
    }

    // ── employerSwipe ─────────────────────────────────────────────────────────

    @Test
    void employerSwipe_savesLikeAndPublishesEvent_whenEmployerOwnsJob() {
        Application application = Application.builder()
                .id(applicationId).candidateId(candidateId).jobId(jobId)
                .build();

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(activeJob));
        doNothing().when(employerSwipeRepository)
                .upsert(employerId, applicationId, candidateId, jobId, "LIKE");

        SwipeResponse result = swipeService.employerSwipe(employerId, applicationId, EmployerSwipeAction.LIKE);

        assertThat(result.getAction()).isEqualTo("LIKE");
        assertThat(result.isRecorded()).isTrue();
        verify(employerSwipeRepository).upsert(employerId, applicationId, candidateId, jobId, "LIKE");
        verify(swipeEventPublisher).publish(any(SwipeEvent.class));
    }

    @Test
    void employerSwipe_throwsAccessDeniedException_whenEmployerDoesNotOwnJob() {
        UUID otherEmployerId = UUID.randomUUID();
        Job jobOwnedByOther = Job.builder()
                .id(jobId).employerId(otherEmployerId)
                .title("SWE").description("Build").category("Eng")
                .employmentType(EmploymentType.FULL_TIME).status(JobStatus.ACTIVE)
                .build();
        Application application = Application.builder()
                .id(applicationId).candidateId(candidateId).jobId(jobId)
                .build();

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(jobOwnedByOther));

        assertThatThrownBy(() -> swipeService.employerSwipe(employerId, applicationId, EmployerSwipeAction.LIKE))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void employerSwipe_throwsResourceNotFoundException_whenApplicationNotFound() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> swipeService.employerSwipe(employerId, applicationId, EmployerSwipeAction.LIKE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getJobLikers ──────────────────────────────────────────────────────────

    @Test
    void getJobLikers_returnsLikers_forPremiumEmployer() {
        CandidateSwipe swipe = CandidateSwipe.builder()
                .id(UUID.randomUUID()).candidateId(candidateId).jobId(jobId)
                .action(SwipeAction.LIKE).build();

        when(subscriptionService.isActive(employerId)).thenReturn(true);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(activeJob));
        when(candidateSwipeRepository.findByJobIdAndAction(jobId, SwipeAction.LIKE))
                .thenReturn(List.of(swipe));

        JobLikersResponse result = swipeService.getJobLikers(employerId, jobId);

        assertThat(result.getLikers()).hasSize(1);
        assertThat(result.getLikers().get(0).getCandidateId()).isEqualTo(candidateId);
    }

    @Test
    void getJobLikers_throwsPremiumRequiredException_forNonPremiumEmployer() {
        when(subscriptionService.isActive(employerId)).thenReturn(false);

        assertThatThrownBy(() -> swipeService.getJobLikers(employerId, jobId))
                .isInstanceOf(PremiumRequiredException.class);
    }

    @Test
    void getJobLikers_returnsEmptyList_whenNoLikes() {
        when(subscriptionService.isActive(employerId)).thenReturn(true);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(activeJob));
        when(candidateSwipeRepository.findByJobIdAndAction(jobId, SwipeAction.LIKE))
                .thenReturn(Collections.emptyList());

        JobLikersResponse result = swipeService.getJobLikers(employerId, jobId);

        assertThat(result.getLikers()).isEmpty();
    }
}
