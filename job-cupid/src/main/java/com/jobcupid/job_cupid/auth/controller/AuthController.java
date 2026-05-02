package com.jobcupid.job_cupid.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobcupid.job_cupid.auth.dto.AuthResponse;
import com.jobcupid.job_cupid.auth.dto.LoginRequest;
import com.jobcupid.job_cupid.auth.dto.LogoutRequest;
import com.jobcupid.job_cupid.auth.dto.RefreshRequest;
import com.jobcupid.job_cupid.auth.dto.RegisterRequest;
import com.jobcupid.job_cupid.auth.service.AuthService;
import com.jobcupid.job_cupid.shared.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /auth/register
     * Register a new candidate or employer account.
     * Returns 201 Created with token pair on success.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /auth/login
     * Authenticate with email + password.
     * Returns 200 OK with token pair on success.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/refresh
     * Rotate the refresh token and issue a new access token.
     * The old refresh token is immediately invalidated (replay protection).
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/logout
     * Blacklists the current access token and revokes the refresh token.
     * Requires a valid Bearer token in the Authorization header.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody LogoutRequest request) {

        // Strip "Bearer " prefix to get the raw token for blacklisting
        String rawAccessToken = authorizationHeader.substring(7);
        authService.logout(rawAccessToken, request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
