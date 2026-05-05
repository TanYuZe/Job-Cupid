package com.jobcupid.job_cupid.application.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.jobcupid.job_cupid.application.entity.Application;
import com.jobcupid.job_cupid.job.entity.EmploymentType;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;
import com.jobcupid.job_cupid.user.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ApplicationRepositoryTest {

    @Autowired ApplicationRepository applicationRepository;
    @Autowired UserRepository         userRepository;
    @Autowired JobRepository          jobRepository;

    private UUID candidateId;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        User candidate = userRepository.saveAndFlush(User.builder()
                .email("candidate-" + UUID.randomUUID() + "@test.com")
                .passwordHash("x").firstName("Alice").lastName("Chen")
                .role(UserRole.USER).build());
        candidateId = candidate.getId();

        Job job = jobRepository.saveAndFlush(Job.builder()
                .employerId(UUID.randomUUID())
                .title("SWE").description("Build stuff").category("Eng")
                .employmentType(EmploymentType.FULL_TIME).build());
        jobId = job.getId();
    }

    private Application buildApplication(UUID candidateId, UUID jobId) {
        return Application.builder()
                .candidateId(candidateId)
                .jobId(jobId)
                .appliedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void existsByCandidateIdAndJobId_returnsTrue_whenApplicationSaved() {
        applicationRepository.saveAndFlush(buildApplication(candidateId, jobId));

        assertThat(applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId)).isTrue();
    }

    @Test
    void existsByCandidateIdAndJobId_returnsFalse_whenNoApplication() {
        assertThat(applicationRepository.existsByCandidateIdAndJobId(UUID.randomUUID(), jobId)).isFalse();
    }

    @Test
    void findByJobId_returnsPageOfThree_whenThreeApplicationsSaved() {
        for (int i = 0; i < 3; i++) {
            User c = userRepository.saveAndFlush(User.builder()
                    .email("c-" + UUID.randomUUID() + "@test.com")
                    .passwordHash("x").firstName("C" + i).lastName("Test")
                    .role(UserRole.USER).build());
            applicationRepository.saveAndFlush(buildApplication(c.getId(), jobId));
        }

        Page<Application> page = applicationRepository.findByJobId(jobId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void save_throwsDataIntegrityViolationException_whenDuplicateCandidateAndJob() {
        applicationRepository.saveAndFlush(buildApplication(candidateId, jobId));

        assertThatThrownBy(() ->
                applicationRepository.saveAndFlush(buildApplication(candidateId, jobId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
