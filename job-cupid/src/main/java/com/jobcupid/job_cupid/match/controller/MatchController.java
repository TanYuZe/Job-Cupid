package com.jobcupid.job_cupid.match.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobcupid.job_cupid.match.dto.MatchResponse;
import com.jobcupid.job_cupid.match.service.MatchService;
import com.jobcupid.job_cupid.shared.security.UserPrincipal;
import com.jobcupid.job_cupid.user.entity.UserRole;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping
    @Secured({"ROLE_USER", "ROLE_EMPLOYER"})
    public ResponseEntity<Page<MatchResponse>> getMatches(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(sort = "matchedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UserRole role = isEmployer(principal) ? UserRole.EMPLOYER : UserRole.USER;
        return ResponseEntity.ok(matchService.getMatchesForUser(principal.getId(), role, pageable));
    }

    @GetMapping("/{id}")
    @Secured({"ROLE_USER", "ROLE_EMPLOYER"})
    public ResponseEntity<MatchResponse> getMatch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(matchService.getMatchById(principal.getId(), id));
    }

    private boolean isEmployer(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYER"));
    }
}
