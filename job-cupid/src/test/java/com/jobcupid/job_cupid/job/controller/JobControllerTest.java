package com.jobcupid.job_cupid.job.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobcupid.job_cupid.auth.service.TokenService;
import com.jobcupid.job_cupid.job.dto.CreateJobRequest;
import com.jobcupid.job_cupid.job.dto.JobResponse;
import com.jobcupid.job_cupid.job.entity.EmploymentType;
import com.jobcupid.job_cupid.job.entity.JobStatus;
import com.jobcupid.job_cupid.job.service.JobService;
import com.jobcupid.job_cupid.shared.security.CustomUserDetailsService;
import com.jobcupid.job_cupid.shared.security.JwtAuthenticationFilter;
import com.jobcupid.job_cupid.shared.security.UserPrincipal;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper mapper;

    @MockitoBean JobService              jobService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean TokenService            tokenService;

    private UUID                              employerId;
    private UUID                              candidateId;
    private UsernamePasswordAuthenticationToken employerAuth;
    private UsernamePasswordAuthenticationToken candidateAuth;

    @BeforeEach
    void setUp() {
        employerId  = UUID.randomUUID();
        candidateId = UUID.randomUUID();

        UserPrincipal employerPrincipal = UserPrincipal.of(User.builder()
                .id(employerId).email("employer@acme.com")
                .passwordHash("x").firstName("Bob").lastName("Smith")
                .role(UserRole.EMPLOYER).isPremium(false).isActive(true).isBanned(false)
                .build());

        UserPrincipal candidatePrincipal = UserPrincipal.of(User.builder()
                .id(candidateId).email("alice@example.com")
                .passwordHash("x").firstName("Alice").lastName("Chen")
                .role(UserRole.USER).isPremium(false).isActive(true).isBanned(false)
                .build());

        employerAuth = new UsernamePasswordAuthenticationToken(
                employerPrincipal, null, employerPrincipal.getAuthorities());
        candidateAuth = new UsernamePasswordAuthenticationToken(
                candidatePrincipal, null, candidatePrincipal.getAuthorities());
    }

    @Test
    void createJob_returns403_forUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createJob_returns201_withJobId_forEmployer() throws Exception {
        UUID jobId = UUID.randomUUID();
        JobResponse response = jobResponse(jobId, employerId);

        when(jobService.createJob(eq(employerId), any(CreateJobRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/jobs")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/jobs/" + jobId))
                .andExpect(jsonPath("$.id").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getMyJobs_returns200_withJobList_forEmployer() throws Exception {
        UUID jobId = UUID.randomUUID();
        JobResponse job = jobResponse(jobId, employerId);

        when(jobService.getMyJobs(eq(employerId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(job)));

        mockMvc.perform(get("/api/v1/jobs/my")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(jobId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── getJobById ────────────────────────────────────────────────────────────

    @Test
    void getJobById_returns200_withJobDetails_forAnyAuthenticatedUser() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(jobService.getJobById(jobId)).thenReturn(jobResponse(jobId, employerId));

        mockMvc.perform(get("/api/v1/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId.toString()))
                .andExpect(jsonPath("$.title").value("Software Engineer"));
    }

    @Test
    void getJobById_returns404_whenJobDoesNotExist() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(jobService.getJobById(jobId))
                .thenThrow(new com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException("Job not found: " + jobId));

        mockMvc.perform(get("/api/v1/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth)))
                .andExpect(status().isNotFound());
    }

    // ── closeJob ──────────────────────────────────────────────────────────────

    @Test
    void closeJob_returns204_whenOwnerDeletesJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        doNothing().when(jobService).closeJob(eq(employerId), eq(jobId));

        mockMvc.perform(delete("/api/v1/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void closeJob_returns403_whenNonOwnerEmployerDeletesJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        doThrow(new org.springframework.security.access.AccessDeniedException("You do not own this job posting"))
                .when(jobService).closeJob(eq(employerId), eq(jobId));

        mockMvc.perform(delete("/api/v1/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(employerAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void closeJob_returns403_whenCandidateAttemptsToDeleteJob() throws Exception {
        UUID jobId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/jobs/{jobId}", jobId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(candidateAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreateJobRequest validCreateRequest() {
        CreateJobRequest req = new CreateJobRequest();
        req.setTitle("Software Engineer");
        req.setDescription("Build great things");
        req.setCategory("Engineering");
        req.setEmploymentType(EmploymentType.FULL_TIME);
        return req;
    }

    private JobResponse jobResponse(UUID jobId, UUID employerId) {
        return JobResponse.builder()
                .id(jobId)
                .employerId(employerId)
                .title("Software Engineer")
                .description("Build great things")
                .category("Engineering")
                .employmentType(EmploymentType.FULL_TIME)
                .status(JobStatus.ACTIVE)
                .boostScore(0)
                .applicationCount(0)
                .isRemote(false)
                .currency("USD")
                .build();
    }
}
