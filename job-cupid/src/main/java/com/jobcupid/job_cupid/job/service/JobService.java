package com.jobcupid.job_cupid.job.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobcupid.job_cupid.job.dto.CreateJobRequest;
import com.jobcupid.job_cupid.job.dto.JobResponse;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.shared.exception.BusinessRuleException;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    @Transactional
    public JobResponse createJob(UUID employerId, CreateJobRequest request) {
        validateSalaryRange(request.getSalaryMin(), request.getSalaryMax());

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
                .expiresAt(request.getExpiresAt())
                .build();

        return JobResponse.from(jobRepository.save(job));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
