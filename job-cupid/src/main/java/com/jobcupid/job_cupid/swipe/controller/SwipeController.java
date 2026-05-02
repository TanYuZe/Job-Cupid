package com.jobcupid.job_cupid.swipe.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobcupid.job_cupid.shared.security.UserPrincipal;
import com.jobcupid.job_cupid.swipe.dto.SwipeRequest;
import com.jobcupid.job_cupid.swipe.dto.SwipeResponse;
import com.jobcupid.job_cupid.swipe.service.SwipeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/swipes")
@RequiredArgsConstructor
public class SwipeController {

    private final SwipeService swipeService;

    @PostMapping("/jobs/{jobId}")
    @Secured("ROLE_USER")
    public ResponseEntity<SwipeResponse> swipeJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId,
            @Valid @RequestBody SwipeRequest request) {
        SwipeResponse response = swipeService.swipeJob(principal.getId(), jobId, request.getAction());
        return ResponseEntity.ok(response);
    }
}
