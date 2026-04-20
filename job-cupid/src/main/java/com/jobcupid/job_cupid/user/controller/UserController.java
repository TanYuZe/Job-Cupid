package com.jobcupid.job_cupid.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobcupid.job_cupid.shared.security.UserPrincipal;
import com.jobcupid.job_cupid.user.dto.UpdateProfileRequest;
import com.jobcupid.job_cupid.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/v1/users/me
     * Returns CandidateProfileResponse for USER role, EmployerProfileResponse for EMPLOYER role.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getId()));
    }

    /**
     * PUT /api/v1/users/me
     * PATCH semantics: only non-null fields are applied.
     * email, role, and isPremium cannot be changed via this endpoint.
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
    }


    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal UserPrincipal principal) {
        userService.softDeleteUser(principal.getId());
        return ResponseEntity.noContent().build();
    }
}
