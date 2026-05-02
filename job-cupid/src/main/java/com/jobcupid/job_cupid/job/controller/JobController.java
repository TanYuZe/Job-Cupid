package com.jobcupid.job_cupid.job.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobcupid.job_cupid.job.dto.CreateJobRequest;
import com.jobcupid.job_cupid.job.dto.JobFeedRequest;
import com.jobcupid.job_cupid.job.dto.JobFeedResponse;
import com.jobcupid.job_cupid.job.dto.JobResponse;
import com.jobcupid.job_cupid.job.dto.UpdateJobRequest;
import com.jobcupid.job_cupid.job.service.JobService;
import com.jobcupid.job_cupid.shared.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    @Secured("ROLE_EMPLOYER")
    public ResponseEntity<JobResponse> createJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateJobRequest request) {
        JobResponse response = jobService.createJob(principal.getId(), request);
        return ResponseEntity
                .created(URI.create("/api/v1/jobs/" + response.getId()))
                .body(response);
    }

    @PutMapping("/{jobId}")
    @Secured("ROLE_EMPLOYER")
    public ResponseEntity<JobResponse> updateJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId,
            @Valid @RequestBody UpdateJobRequest request) {
        return ResponseEntity.ok(jobService.updateJob(principal.getId(), jobId, request));
    }

    @GetMapping("/my")
    @Secured("ROLE_EMPLOYER")
    public ResponseEntity<?> getMyJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(jobService.getMyJobs(principal.getId(), pageable));
    }

    @GetMapping
    @Secured("ROLE_USER")
    public ResponseEntity<JobFeedResponse> getJobFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute JobFeedRequest request) {
        return ResponseEntity.ok(jobService.getJobFeed(principal.getId(), request));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable UUID jobId) {
        return ResponseEntity.ok(jobService.getJobById(jobId));
    }

    @DeleteMapping("/{jobId}")
    @Secured("ROLE_EMPLOYER")
    public ResponseEntity<Void> closeJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {
        jobService.closeJob(principal.getId(), jobId);
        return ResponseEntity.noContent().build();
    }
}
