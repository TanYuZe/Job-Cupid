package com.jobcupid.job_cupid.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.jobcupid.job_cupid.job.dto.CreateJobRequest;
import com.jobcupid.job_cupid.job.dto.JobResponse;
import com.jobcupid.job_cupid.job.dto.UpdateJobRequest;
import com.jobcupid.job_cupid.job.entity.EmploymentType;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.entity.JobStatus;
import com.jobcupid.job_cupid.job.repository.JobFeedRepository;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;
import com.jobcupid.job_cupid.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock JobRepository     jobRepository;
    @Mock JobFeedRepository jobFeedRepository;
    @Mock UserRepository    userRepository;

    @InjectMocks JobService jobService;

    // ── createJob ─────────────────────────────────────────────────────────────

    @Test
    void createJob_savesJobWithStatusActiveAndBoostScoreZero_forNonPremiumEmployer() {
        UUID employerId = UUID.randomUUID();
        when(userRepository.findById(employerId)).thenReturn(Optional.of(employerUser(employerId, false)));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse response = jobService.createJob(employerId, validCreateRequest());

        assertThat(response.getStatus()).isEqualTo(JobStatus.ACTIVE);
        assertThat(response.getBoostScore()).isEqualTo(0);
    }

    @Test
    void createJob_savesJobWithBoostScore100_forPremiumEmployer() {
        UUID employerId = UUID.randomUUID();
        when(userRepository.findById(employerId)).thenReturn(Optional.of(employerUser(employerId, true)));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse response = jobService.createJob(employerId, validCreateRequest());

        assertThat(response.getBoostScore()).isEqualTo(100);
    }

    // ── updateJob ─────────────────────────────────────────────────────────────

    @Test
    void updateJob_throwsAccessDeniedException_whenEmployerDoesNotOwnJob() {
        UUID ownerId    = UUID.randomUUID();
        UUID intruderId = UUID.randomUUID();
        UUID jobId      = UUID.randomUUID();

        Job job = Job.builder()
                .employerId(ownerId)
                .title("Original")
                .description("Desc")
                .category("Tech")
                .employmentType(EmploymentType.FULL_TIME)
                .build();

        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.updateJob(intruderId, jobId, new UpdateJobRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateJob_appliesOnlyProvidedFields_leavingOthersUnchanged() {
        UUID employerId = UUID.randomUUID();
        UUID jobId      = UUID.randomUUID();

        Job job = Job.builder()
                .employerId(employerId)
                .title("Original Title")
                .description("Original Desc")
                .category("Engineering")
                .location("Singapore")
                .employmentType(EmploymentType.FULL_TIME)
                .build();

        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateJobRequest request = new UpdateJobRequest();
        request.setTitle("Updated Title");

        JobResponse response = jobService.updateJob(employerId, jobId, request);

        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(response.getDescription()).isEqualTo("Original Desc");
        assertThat(response.getLocation()).isEqualTo("Singapore");
    }

    // ── getJobById ────────────────────────────────────────────────────────────

    @Test
    void getJobById_returnsJobResponse_whenJobExists() {
        UUID jobId = UUID.randomUUID();
        Job job = activeJob(UUID.randomUUID(), jobId);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));

        JobResponse response = jobService.getJobById(jobId);

        assertThat(response.getId()).isEqualTo(jobId);
        assertThat(response.getTitle()).isEqualTo("Software Engineer");
    }

    @Test
    void getJobById_throwsResourceNotFoundException_whenJobDoesNotExist() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobById(jobId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── closeJob ──────────────────────────────────────────────────────────────

    @Test
    void closeJob_throwsAccessDeniedException_whenCallerDoesNotOwnJob() {
        UUID ownerId    = UUID.randomUUID();
        UUID intruderId = UUID.randomUUID();
        UUID jobId      = UUID.randomUUID();
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId))
                .thenReturn(Optional.of(activeJob(ownerId, jobId)));

        assertThatThrownBy(() -> jobService.closeJob(intruderId, jobId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void closeJob_setsStatusClosedAndDeletedAt_whenCallerOwnsJob() {
        UUID employerId = UUID.randomUUID();
        UUID jobId      = UUID.randomUUID();
        Job job = activeJob(employerId, jobId);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        jobService.closeJob(employerId, jobId);

        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
        assertThat(job.getDeletedAt()).isNotNull();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Job activeJob(UUID employerId, UUID jobId) {
        return Job.builder()
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



    private User employerUser(UUID id, boolean isPremium) {
        return User.builder()
                .id(id)
                .email(id + "@employer.com")
                .passwordHash("$2a$12$x")
                .firstName("Employer")
                .lastName("Test")
                .role(UserRole.EMPLOYER)
                .isPremium(isPremium)
                .isActive(true)
                .isBanned(false)
                .build();
    }

    private CreateJobRequest validCreateRequest() {
        CreateJobRequest req = new CreateJobRequest();
        req.setTitle("Software Engineer");
        req.setDescription("Build great things");
        req.setCategory("Engineering");
        req.setEmploymentType(EmploymentType.FULL_TIME);
        return req;
    }
}
