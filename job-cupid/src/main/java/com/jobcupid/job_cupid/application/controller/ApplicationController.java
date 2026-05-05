package com.jobcupid.job_cupid.application.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobcupid.job_cupid.application.dto.ApplyRequest;
import com.jobcupid.job_cupid.application.dto.ApplicationResponse;
import com.jobcupid.job_cupid.application.dto.UpdateStatusRequest;
import com.jobcupid.job_cupid.application.service.ApplicationService;
import com.jobcupid.job_cupid.shared.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/jobs/{jobId}")
    @Secured("ROLE_USER")
    public ResponseEntity<ApplicationResponse> apply(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId,
            @RequestBody(required = false) ApplyRequest request) {
        ApplicationResponse response = applicationService.apply(
                principal.getId(), jobId, request != null ? request : new ApplyRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @Secured("ROLE_USER")
    public ResponseEntity<Page<ApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        return ResponseEntity.ok(applicationService.getCandidateApplications(principal.getId(), pageable));
    }

    @GetMapping("/jobs/{jobId}")
    @Secured("ROLE_EMPLOYER")
    public ResponseEntity<Page<ApplicationResponse>> getJobApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId,
            Pageable pageable) {
        return ResponseEntity.ok(applicationService.getJobApplications(principal.getId(), jobId, pageable));
    }

    @PutMapping("/{id}/status")
    @Secured("ROLE_EMPLOYER")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(
                applicationService.updateStatus(principal.getId(), id, request.getNewStatus()));
    }
}
