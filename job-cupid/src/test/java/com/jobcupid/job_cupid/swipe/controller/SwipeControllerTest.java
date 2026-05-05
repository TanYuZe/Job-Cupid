package com.jobcupid.job_cupid.swipe.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.jobcupid.job_cupid.auth.service.TokenService;
import com.jobcupid.job_cupid.shared.exception.PremiumRequiredException;
import com.jobcupid.job_cupid.shared.exception.SwipeLimitExceededException;
import com.jobcupid.job_cupid.shared.security.CustomUserDetailsService;
import com.jobcupid.job_cupid.shared.security.UserPrincipal;
import com.jobcupid.job_cupid.swipe.dto.JobLikersResponse;
import com.jobcupid.job_cupid.swipe.dto.SwipeResponse;
import com.jobcupid.job_cupid.swipe.entity.EmployerSwipeAction;
import com.jobcupid.job_cupid.swipe.entity.SwipeAction;
import com.jobcupid.job_cupid.swipe.service.SwipeService;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;

@WebMvcTest(SwipeController.class)
class SwipeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean SwipeService             swipeService;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean TokenService             tokenService;

    private UUID                                candidateId;
    private UUID                                employerId;
    private UUID                                jobId;
    private UUID                                applicationId;
    private UsernamePasswordAuthenticationToken candidateAuth;
    private UsernamePasswordAuthenticationToken employerAuth;

    @BeforeEach
    void setUp() {
        candidateId   = UUID.randomUUID();
        employerId    = UUID.randomUUID();
        jobId         = UUID.randomUUID();
        applicationId = UUID.randomUUID();

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

    // ── candidateSwipe ────────────────────────────────────────────────────────

    @Test
    void swipeJob_returns200_forValidUserWithActiveJob() throws Exception {
        when(swipeService.candidateSwipe(eq(candidateId), eq(jobId), eq(SwipeAction.LIKE)))
                .thenReturn(SwipeResponse.builder().action("LIKE").recorded(true).build());

        mockMvc.perform(post("/api/v1/swipes/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"LIKE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("LIKE"))
                .andExpect(jsonPath("$.recorded").value(true));
    }

    @Test
    void swipeJob_returns429WithUpgradeUrl_whenFreeUserExceedsLimit() throws Exception {
        doThrow(new SwipeLimitExceededException())
                .when(swipeService).candidateSwipe(any(UUID.class), any(UUID.class), any(SwipeAction.class));

        mockMvc.perform(post("/api/v1/swipes/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"LIKE\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.upgradeUrl").value("/api/v1/subscriptions/plans"));
    }

    @Test
    void swipeJob_returns403_forEmployerRole() throws Exception {
        mockMvc.perform(post("/api/v1/swipes/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"LIKE\"}"))
                .andExpect(status().isForbidden());
    }

    // ── employerSwipe ─────────────────────────────────────────────────────────

    @Test
    void swipeApplicant_returns200_forEmployerWithOwnApplicant() throws Exception {
        when(swipeService.employerSwipe(eq(employerId), eq(applicationId), eq(EmployerSwipeAction.LIKE)))
                .thenReturn(SwipeResponse.builder().action("LIKE").recorded(true).build());

        mockMvc.perform(post("/api/v1/swipes/applicants/{applicationId}", applicationId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"LIKE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("LIKE"))
                .andExpect(jsonPath("$.recorded").value(true));
    }

    @Test
    void swipeApplicant_returns403_forUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/swipes/applicants/{applicationId}", applicationId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"LIKE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void swipeApplicant_returns403_whenEmployerDoesNotOwnApplicant() throws Exception {
        doThrow(new AccessDeniedException("You do not own this job posting"))
                .when(swipeService).employerSwipe(any(UUID.class), any(UUID.class), any(EmployerSwipeAction.class));

        mockMvc.perform(post("/api/v1/swipes/applicants/{applicationId}", applicationId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"LIKE\"}"))
                .andExpect(status().isForbidden());
    }

    // ── getJobLikers ──────────────────────────────────────────────────────────

    @Test
    void getJobLikers_returns200_forEmployerWithPremiumSubscription() throws Exception {
        when(swipeService.getJobLikers(eq(employerId), eq(jobId)))
                .thenReturn(JobLikersResponse.builder().jobId(jobId).likers(List.of()).build());

        mockMvc.perform(get("/api/v1/swipes/jobs/{jobId}/likes", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likers").isArray());
    }

    @Test
    void getJobLikers_returns403WithUpgradeUrl_forNonPremiumEmployer() throws Exception {
        doThrow(new PremiumRequiredException())
                .when(swipeService).getJobLikers(any(UUID.class), any(UUID.class));

        mockMvc.perform(get("/api/v1/swipes/jobs/{jobId}/likes", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.upgradeUrl").value("/api/v1/subscriptions/plans"));
    }

    @TestConfiguration
    static class TestWebMvcConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
