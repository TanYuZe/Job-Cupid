package com.jobcupid.job_cupid.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jobcupid.job_cupid.application.dto.ApplyRequest;
import com.jobcupid.job_cupid.application.dto.ApplicationResponse;
import com.jobcupid.job_cupid.application.service.ApplicationService;
import com.jobcupid.job_cupid.auth.service.TokenService;
import com.jobcupid.job_cupid.shared.exception.BusinessRuleException;
import com.jobcupid.job_cupid.shared.exception.DuplicateApplicationException;
import com.jobcupid.job_cupid.shared.security.CustomUserDetailsService;
import com.jobcupid.job_cupid.shared.security.JwtAuthenticationFilter;
import com.jobcupid.job_cupid.shared.security.UserPrincipal;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ApplicationService       applicationService;
    @MockitoBean JwtAuthenticationFilter  jwtAuthenticationFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean TokenService             tokenService;

    private UUID                                candidateId;
    private UUID                                employerId;
    private UUID                                jobId;
    private UsernamePasswordAuthenticationToken candidateAuth;
    private UsernamePasswordAuthenticationToken employerAuth;

    @BeforeEach
    void setUp() {
        candidateId = UUID.randomUUID();
        employerId  = UUID.randomUUID();
        jobId       = UUID.randomUUID();

        UserPrincipal candidatePrincipal = UserPrincipal.of(User.builder()
                .id(candidateId).email("alice@example.com")
                .passwordHash("x").firstName("Alice").lastName("Chen")
                .role(UserRole.USER).isPremium(false).isActive(true).isBanned(false)
                .build());

        UserPrincipal employerPrincipal = UserPrincipal.of(User.builder()
                .id(employerId).email("employer@acme.com")
                .passwordHash("x").firstName("Bob").lastName("Smith")
                .role(UserRole.EMPLOYER).isPremium(false).isActive(true).isBanned(false)
                .build());

        candidateAuth = new UsernamePasswordAuthenticationToken(
                candidatePrincipal, null, candidatePrincipal.getAuthorities());
        employerAuth = new UsernamePasswordAuthenticationToken(
                employerPrincipal, null, employerPrincipal.getAuthorities());
    }

    // ── apply ──────────────────────────────────────────────────────────────────

    @Test
    void apply_returns201_forUserWithPriorLikeSwipe() throws Exception {
        when(applicationService.apply(eq(candidateId), eq(jobId), any(ApplyRequest.class)))
                .thenReturn(ApplicationResponse.builder()
                        .id(UUID.randomUUID()).candidateId(candidateId).jobId(jobId)
                        .status("PENDING").appliedAt(Instant.now())
                        .build());

        mockMvc.perform(post("/api/v1/applications/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void apply_returns422_whenNoPriorLikeSwipe() throws Exception {
        doThrow(new BusinessRuleException("You must swipe LIKE on a job before applying"))
                .when(applicationService).apply(any(UUID.class), any(UUID.class), any());

        mockMvc.perform(post("/api/v1/applications/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is(422));
    }

    @Test
    void apply_returns409_whenDuplicateApplication() throws Exception {
        doThrow(new DuplicateApplicationException())
                .when(applicationService).apply(any(UUID.class), any(UUID.class), any());

        mockMvc.perform(post("/api/v1/applications/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void apply_returns403_forEmployerRole() throws Exception {
        mockMvc.perform(post("/api/v1/applications/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ── getMyApplications ─────────────────────────────────────────────────────

    @Test
    void getMyApplications_returns200WithEmptyPage_forCandidateRole() throws Exception {
        when(applicationService.getCandidateApplications(eq(candidateId), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/applications/my")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ── getJobApplications ────────────────────────────────────────────────────

    @Test
    void getJobApplications_returns200WithApplicants_forEmployerWithOwnJob() throws Exception {
        when(applicationService.getJobApplications(eq(employerId), eq(jobId), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/applications/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getJobApplications_returns403_whenNonOwnerEmployer() throws Exception {
        doThrow(new AccessDeniedException("You do not own this job"))
                .when(applicationService)
                .getJobApplications(any(UUID.class), any(UUID.class), any(Pageable.class));

        mockMvc.perform(get("/api/v1/applications/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }
}
