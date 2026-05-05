package com.jobcupid.job_cupid.match.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jobcupid.job_cupid.auth.service.TokenService;
import com.jobcupid.job_cupid.match.dto.MatchResponse;
import com.jobcupid.job_cupid.match.service.MatchService;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.shared.security.CustomUserDetailsService;
import com.jobcupid.job_cupid.shared.security.JwtAuthenticationFilter;
import com.jobcupid.job_cupid.shared.security.UserPrincipal;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;

import java.util.List;

@WebMvcTest(MatchController.class)
class MatchControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean MatchService             matchService;
    @MockitoBean JwtAuthenticationFilter  jwtAuthenticationFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean TokenService             tokenService;

    private UUID                                candidateId;
    private UUID                                employerId;
    private UsernamePasswordAuthenticationToken candidateAuth;
    private UsernamePasswordAuthenticationToken employerAuth;

    @BeforeEach
    void setUp() {
        candidateId = UUID.randomUUID();
        employerId  = UUID.randomUUID();

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

    // ── GET /api/v1/matches ───────────────────────────────────────────────────

    @Test
    void getMatches_returns200WithMatchList_forCandidateWithMatches() throws Exception {
        UUID matchId = UUID.randomUUID();
        MatchResponse response = MatchResponse.builder()
                .id(matchId).candidateId(candidateId).employerId(employerId)
                .jobId(UUID.randomUUID()).applicationId(UUID.randomUUID())
                .status("ACTIVE").matchedAt(Instant.now())
                .build();

        when(matchService.getMatchesForUser(eq(candidateId), eq(UserRole.USER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/matches")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(matchId.toString()));
    }

    @Test
    void getMatches_returns200WithEmptyPage_forEmployerWithNoMatches() throws Exception {
        when(matchService.getMatchesForUser(eq(employerId), eq(UserRole.EMPLOYER), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/matches")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ── GET /api/v1/matches/{id} ──────────────────────────────────────────────

    @Test
    void getMatch_returns404_whenUserIsNotParticipant() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.getMatchById(eq(candidateId), eq(matchId)))
                .thenThrow(new ResourceNotFoundException("Match not found: " + matchId));

        mockMvc.perform(get("/api/v1/matches/{id}", matchId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMatch_returns200_whenUserIsParticipant() throws Exception {
        UUID matchId = UUID.randomUUID();
        MatchResponse response = MatchResponse.builder()
                .id(matchId).candidateId(candidateId).employerId(employerId)
                .jobId(UUID.randomUUID()).applicationId(UUID.randomUUID())
                .status("ACTIVE").matchedAt(Instant.now())
                .build();
        when(matchService.getMatchById(eq(candidateId), eq(matchId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/matches/{id}", matchId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(matchId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
