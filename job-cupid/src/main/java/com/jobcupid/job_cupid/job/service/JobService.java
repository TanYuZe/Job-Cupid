package com.jobcupid.job_cupid.job.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobcupid.job_cupid.job.dto.CreateJobRequest;
import com.jobcupid.job_cupid.job.dto.JobFeedRequest;
import com.jobcupid.job_cupid.job.dto.JobFeedResponse;
import com.jobcupid.job_cupid.job.dto.JobResponse;
import com.jobcupid.job_cupid.job.dto.UpdateJobRequest;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.entity.JobStatus;
import com.jobcupid.job_cupid.job.repository.FeedCursor;
import com.jobcupid.job_cupid.job.repository.JobFeedRepository;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.shared.exception.BusinessRuleException;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository     jobRepository;
    private final JobFeedRepository jobFeedRepository;
    private final UserRepository    userRepository;

    @Transactional
    public JobResponse createJob(UUID employerId, CreateJobRequest request) {
        validateSalaryRange(request.getSalaryMin(), request.getSalaryMax());

        User employer = userRepository.findById(employerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employer not found: " + employerId));

        int boostScore = Boolean.TRUE.equals(employer.getIsPremium()) ? 100 : 0;

        Job job = Job.builder()
                .employerId(employerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .location(request.getLocation())
                .isRemote(request.getIsRemote() != null ? request.getIsRemote() : false)
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .employmentType(request.getEmploymentType())
                .experienceLevel(request.getExperienceLevel())
                .requiredSkills(request.getRequiredSkills())
                .boostScore(boostScore)
                .expiresAt(request.getExpiresAt())
                .build();

        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional
    public JobResponse updateJob(UUID employerId, UUID jobId, UpdateJobRequest request) {
        Job job = loadActiveJob(jobId);

        if (!job.getEmployerId().equals(employerId)) {
            throw new AccessDeniedException("You do not own this job posting");
        }

        validateSalaryRange(
                request.getSalaryMin() != null ? request.getSalaryMin() : job.getSalaryMin(),
                request.getSalaryMax() != null ? request.getSalaryMax() : job.getSalaryMax());

        applyJobFields(job, request);
        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getMyJobs(UUID employerId, Pageable pageable) {
        return jobRepository.findByEmployerIdAndDeletedAtIsNull(employerId, pageable)
                .map(JobResponse::from);
    }

    @Transactional(readOnly = true)
    public JobFeedResponse getJobFeed(UUID candidateId, JobFeedRequest request) {
        int fetchSize = request.getSize() + 1;
        List<Job> jobs = jobFeedRepository.findFeed(candidateId, request, fetchSize);

        boolean hasMore = jobs.size() == fetchSize;
        if (hasMore) jobs = jobs.subList(0, fetchSize - 1);

        List<JobResponse> items = jobs.stream().map(JobResponse::from).toList();
        String nextCursor = hasMore && !jobs.isEmpty()
                ? FeedCursor.from(jobs.get(jobs.size() - 1)).encode() : null;

        return JobFeedResponse.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID jobId) {
        return JobResponse.from(loadActiveJob(jobId));
    }

    @Transactional
    public void closeJob(UUID employerId, UUID jobId) {
        Job job = loadActiveJob(jobId);

        if (!job.getEmployerId().equals(employerId)) {
            throw new AccessDeniedException("You do not own this job posting");
        }

        job.setStatus(JobStatus.CLOSED);
        job.setDeletedAt(OffsetDateTime.now());
        jobRepository.save(job);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyJobFields(Job job, UpdateJobRequest req) {
        if (req.getTitle()           != null) job.setTitle(req.getTitle());
        if (req.getDescription()     != null) job.setDescription(req.getDescription());
        if (req.getCategory()        != null) job.setCategory(req.getCategory());
        if (req.getLocation()        != null) job.setLocation(req.getLocation());
        if (req.getIsRemote()        != null) job.setIsRemote(req.getIsRemote());
        if (req.getSalaryMin()       != null) job.setSalaryMin(req.getSalaryMin());
        if (req.getSalaryMax()       != null) job.setSalaryMax(req.getSalaryMax());
        if (req.getCurrency()        != null) job.setCurrency(req.getCurrency());
        if (req.getEmploymentType()  != null) job.setEmploymentType(req.getEmploymentType());
        if (req.getExperienceLevel() != null) job.setExperienceLevel(req.getExperienceLevel());
        if (req.getRequiredSkills()  != null) job.setRequiredSkills(req.getRequiredSkills());
        if (req.getStatus()          != null) job.setStatus(req.getStatus());
        if (req.getExpiresAt()       != null) job.setExpiresAt(req.getExpiresAt());
    }

    private Job loadActiveJob(UUID jobId) {
        return jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
    }

    private void validateSalaryRange(Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw new BusinessRuleException(
                    "salaryMin (" + min + ") must not exceed salaryMax (" + max + ")");
        }
    }
}
