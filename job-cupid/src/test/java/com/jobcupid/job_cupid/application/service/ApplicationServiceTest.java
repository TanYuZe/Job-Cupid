package com.jobcupid.job_cupid.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.jobcupid.job_cupid.application.dto.ApplyRequest;
import com.jobcupid.job_cupid.application.dto.ApplicationResponse;
import com.jobcupid.job_cupid.application.entity.Application;
import com.jobcupid.job_cupid.application.entity.ApplicationStatus;
import com.jobcupid.job_cupid.application.event.ApplicationEvent;
import com.jobcupid.job_cupid.application.event.ApplicationEventPublisher;
import com.jobcupid.job_cupid.application.repository.ApplicationRepository;
import com.jobcupid.job_cupid.job.entity.EmploymentType;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.entity.JobStatus;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.shared.exception.BusinessRuleException;
import com.jobcupid.job_cupid.shared.exception.DuplicateApplicationException;
import com.jobcupid.job_cupid.shared.exception.JobNotAvailableException;
import com.jobcupid.job_cupid.swipe.entity.SwipeAction;
import com.jobcupid.job_cupid.swipe.repository.CandidateSwipeRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock ApplicationRepository     applicationRepository;
    @Mock CandidateSwipeRepository  candidateSwipeRepository;
    @Mock JobRepository             jobRepository;
    @Mock ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks ApplicationService applicationService;

    private UUID         candidateId;
    private UUID         employerId;
    private UUID         jobId;
    private UUID         applicationId;
    private Job          activeJob;
    private ApplyRequest emptyRequest;

    @BeforeEach
    void setUp() {
        candidateId   = UUID.randomUUID();
        employerId    = UUID.randomUUID();
        jobId         = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        emptyRequest  = new ApplyRequest();

        activeJob = Job.builder()
                .id(jobId).employerId(employerId)
                .title("SWE").description("Build").category("Eng")
                .employmentType(EmploymentType.FULL_TIME).status(JobStatus.ACTIVE)
                .build();
    }

    // ── apply ──────────────────────────────────────────────────────────────────

    @Test
    void apply_savesApplicationAndPublishesEvent_whenCandidatePreviouslyLiked() {
        Application saved = Application.builder()
                .id(UUID.randomUUID()).candidateId(candidateId).jobId(jobId)
                .status(ApplicationStatus.PENDING)
                .appliedAt(OffsetDateTime.now())
                .build();

        when(candidateSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, SwipeAction.LIKE)).thenReturn(true);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(activeJob));
        when(applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenReturn(saved);

        ApplicationResponse result = applicationService.apply(candidateId, jobId, emptyRequest);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getCandidateId()).isEqualTo(candidateId);
        verify(applicationRepository).save(any(Application.class));
        verify(jobRepository).incrementApplicationCount(jobId);
        verify(applicationEventPublisher).publish(any(ApplicationEvent.class));
    }

    @Test
    void apply_throwsBusinessRuleException_whenCandidateSwiped_PASS() {
        when(candidateSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, SwipeAction.LIKE)).thenReturn(false);

        assertThatThrownBy(() -> applicationService.apply(candidateId, jobId, emptyRequest))
                .isInstanceOf(BusinessRuleException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void apply_throwsBusinessRuleException_whenCandidateNeverSwiped() {
        when(candidateSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, SwipeAction.LIKE)).thenReturn(false);

        assertThatThrownBy(() -> applicationService.apply(candidateId, jobId, emptyRequest))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void apply_throwsDuplicateApplicationException_whenAlreadyApplied() {
        when(candidateSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, SwipeAction.LIKE)).thenReturn(true);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(activeJob));
        when(applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(candidateId, jobId, emptyRequest))
                .isInstanceOf(DuplicateApplicationException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void apply_throwsJobNotAvailableException_whenJobClosed() {
        Job closedJob = Job.builder()
                .id(jobId).employerId(employerId)
                .title("SWE").description("Build").category("Eng")
                .employmentType(EmploymentType.FULL_TIME).status(JobStatus.CLOSED)
                .build();

        when(candidateSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, SwipeAction.LIKE)).thenReturn(true);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(closedJob));

        assertThatThrownBy(() -> applicationService.apply(candidateId, jobId, emptyRequest))
                .isInstanceOf(JobNotAvailableException.class);

        verify(applicationRepository, never()).save(any());
    }

    // ── getCandidateApplications ───────────────────────────────────────────────

    @Test
    void getCandidateApplications_returnsPageOf3_whenCandidateHas3Applications() {
        List<Application> apps = List.of(
                Application.builder().id(UUID.randomUUID()).candidateId(candidateId).jobId(jobId)
                        .status(ApplicationStatus.PENDING).appliedAt(OffsetDateTime.now()).build(),
                Application.builder().id(UUID.randomUUID()).candidateId(candidateId).jobId(UUID.randomUUID())
                        .status(ApplicationStatus.PENDING).appliedAt(OffsetDateTime.now()).build(),
                Application.builder().id(UUID.randomUUID()).candidateId(candidateId).jobId(UUID.randomUUID())
                        .status(ApplicationStatus.REVIEWED).appliedAt(OffsetDateTime.now()).build()
        );
        Pageable pageable = PageRequest.of(0, 10);
        when(applicationRepository.findByCandidateId(candidateId, pageable))
                .thenReturn(new PageImpl<>(apps, pageable, 3));

        Page<ApplicationResponse> result = applicationService.getCandidateApplications(candidateId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    // ── getJobApplications ────────────────────────────────────────────────────

    @Test
    void getJobApplications_returnsApplicants_whenEmployerOwnsJob() {
        Application app = Application.builder()
                .id(applicationId).candidateId(candidateId).jobId(jobId)
                .status(ApplicationStatus.PENDING).appliedAt(OffsetDateTime.now()).build();
        Pageable pageable = PageRequest.of(0, 10);

        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(activeJob));
        when(applicationRepository.findByJobId(jobId, pageable))
                .thenReturn(new PageImpl<>(List.of(app), pageable, 1));

        Page<ApplicationResponse> result = applicationService.getJobApplications(employerId, jobId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getCandidateId()).isEqualTo(candidateId);
    }

    @Test
    void getJobApplications_throwsAccessDeniedException_whenEmployerDoesNotOwnJob() {
        UUID otherEmployer = UUID.randomUUID();
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(activeJob));

        assertThatThrownBy(() ->
                applicationService.getJobApplications(otherEmployer, jobId, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    void updateStatus_changesStatusAndPublishesEvent_whenEmployerUpdatesToReviewed() {
        Application app = Application.builder()
                .id(applicationId).candidateId(candidateId).jobId(jobId)
                .status(ApplicationStatus.PENDING).appliedAt(OffsetDateTime.now()).build();
        Application updated = Application.builder()
                .id(applicationId).candidateId(candidateId).jobId(jobId)
                .status(ApplicationStatus.REVIEWED).appliedAt(OffsetDateTime.now())
                .reviewedAt(OffsetDateTime.now()).build();

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(app));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(activeJob));
        when(applicationRepository.save(app)).thenReturn(updated);

        ApplicationResponse result = applicationService.updateStatus(
                employerId, applicationId, ApplicationStatus.REVIEWED);

        assertThat(result.getStatus()).isEqualTo("REVIEWED");
        verify(applicationRepository).save(app);
        verify(applicationEventPublisher).publish(any(ApplicationEvent.class));
    }

    @Test
    void updateStatus_throwsBusinessRuleException_whenEmployerTriesToSetWithdrawn() {
        assertThatThrownBy(() ->
                applicationService.updateStatus(employerId, applicationId, ApplicationStatus.WITHDRAWN))
                .isInstanceOf(BusinessRuleException.class);

        verify(applicationRepository, never()).findById(any());
    }
}
