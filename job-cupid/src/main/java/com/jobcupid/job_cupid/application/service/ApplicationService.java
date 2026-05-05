package com.jobcupid.job_cupid.application.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobcupid.job_cupid.application.dto.ApplyRequest;
import com.jobcupid.job_cupid.application.dto.ApplicationResponse;
import com.jobcupid.job_cupid.application.entity.Application;
import com.jobcupid.job_cupid.application.entity.ApplicationStatus;
import com.jobcupid.job_cupid.application.event.ApplicationEvent;
import com.jobcupid.job_cupid.application.event.ApplicationEventPublisher;
import com.jobcupid.job_cupid.application.repository.ApplicationRepository;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.entity.JobStatus;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.shared.exception.BusinessRuleException;
import com.jobcupid.job_cupid.shared.exception.DuplicateApplicationException;
import com.jobcupid.job_cupid.shared.exception.JobNotAvailableException;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.swipe.entity.SwipeAction;
import com.jobcupid.job_cupid.swipe.repository.CandidateSwipeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository     applicationRepository;
    private final CandidateSwipeRepository  candidateSwipeRepository;
    private final JobRepository             jobRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    // ── apply ──────────────────────────────────────────────────────────────────

    @Transactional
    public ApplicationResponse apply(UUID candidateId, UUID jobId, ApplyRequest request) {
        if (!candidateSwipeRepository.existsByCandidateIdAndJobIdAndAction(
                candidateId, jobId, SwipeAction.LIKE)) {
            throw new BusinessRuleException("You must swipe LIKE on a job before applying");
        }

        Job job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.ACTIVE) {
            throw new JobNotAvailableException("Job is no longer accepting applications");
        }

        if (applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId)) {
            throw new DuplicateApplicationException();
        }

        Application saved = applicationRepository.save(Application.builder()
                .candidateId(candidateId)
                .jobId(jobId)
                .coverLetter(request != null ? request.getCoverLetter() : null)
                .resumeUrl(request != null ? request.getResumeUrl() : null)
                .appliedAt(OffsetDateTime.now())
                .build());

        jobRepository.incrementApplicationCount(jobId);

        applicationEventPublisher.publish(new ApplicationEvent(
                UUID.randomUUID().toString(),
                candidateId,
                jobId,
                saved.getId(),
                saved.getStatus().name(),
                Instant.now()
        ));

        return toResponse(saved);
    }

    // ── getCandidateApplications ───────────────────────────────────────────────

    public Page<ApplicationResponse> getCandidateApplications(UUID candidateId, Pageable pageable) {
        return applicationRepository.findByCandidateId(candidateId, pageable).map(this::toResponse);
    }

    // ── getJobApplications ────────────────────────────────────────────────────

    public Page<ApplicationResponse> getJobApplications(UUID employerId, UUID jobId, Pageable pageable) {
        Job job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (!job.getEmployerId().equals(employerId)) {
            throw new AccessDeniedException("You do not own this job");
        }

        return applicationRepository.findByJobId(jobId, pageable).map(this::toResponse);
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Transactional
    public ApplicationResponse updateStatus(UUID employerId, UUID applicationId, ApplicationStatus newStatus) {
        if (newStatus == ApplicationStatus.WITHDRAWN) {
            throw new BusinessRuleException("WITHDRAWN status can only be set by the candidate");
        }

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        Job job = jobRepository.findByIdAndDeletedAtIsNull(app.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + app.getJobId()));

        if (!job.getEmployerId().equals(employerId)) {
            throw new AccessDeniedException("You do not own this job");
        }

        app.setStatus(newStatus);
        app.setReviewedAt(OffsetDateTime.now());
        Application saved = applicationRepository.save(app);

        applicationEventPublisher.publish(new ApplicationEvent(
                UUID.randomUUID().toString(),
                app.getCandidateId(),
                app.getJobId(),
                app.getId(),
                newStatus.name(),
                Instant.now()
        ));

        return toResponse(saved);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ApplicationResponse toResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .candidateId(app.getCandidateId())
                .jobId(app.getJobId())
                .coverLetter(app.getCoverLetter())
                .resumeUrl(app.getResumeUrl())
                .status(app.getStatus().name())
                .appliedAt(app.getAppliedAt() != null ? app.getAppliedAt().toInstant() : null)
                .build();
    }
}
