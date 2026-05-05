package com.jobcupid.job_cupid.match.repository;

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
import com.jobcupid.job_cupid.application.repository.ApplicationRepository;
import com.jobcupid.job_cupid.job.entity.EmploymentType;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.match.entity.Match;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;
import com.jobcupid.job_cupid.user.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class MatchRepositoryTest {

    @Autowired MatchRepository       matchRepository;
    @Autowired UserRepository        userRepository;
    @Autowired JobRepository         jobRepository;
    @Autowired ApplicationRepository applicationRepository;

    private UUID candidateId;
    private UUID employerId;
    private UUID jobId;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        User candidate = userRepository.saveAndFlush(User.builder()
                .email("candidate-" + UUID.randomUUID() + "@test.com")
                .passwordHash("x").firstName("Alice").lastName("Chen")
                .role(UserRole.USER).build());
        candidateId = candidate.getId();

        User employer = userRepository.saveAndFlush(User.builder()
                .email("employer-" + UUID.randomUUID() + "@test.com")
                .passwordHash("x").firstName("Bob").lastName("Smith")
                .role(UserRole.EMPLOYER).build());
        employerId = employer.getId();

        Job job = jobRepository.saveAndFlush(Job.builder()
                .employerId(employerId)
                .title("SWE").description("Build stuff").category("Eng")
                .employmentType(EmploymentType.FULL_TIME).build());
        jobId = job.getId();

        Application application = applicationRepository.saveAndFlush(Application.builder()
                .candidateId(candidateId).jobId(jobId)
                .appliedAt(OffsetDateTime.now()).build());
        applicationId = application.getId();
    }

    private Match buildMatch(UUID candidateId, UUID jobId) {
        return Match.builder()
                .candidateId(candidateId)
                .employerId(employerId)
                .jobId(jobId)
                .applicationId(applicationId)
                .matchedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void findByCandidateId_returnsMatch_whenMatchSaved() {
        matchRepository.saveAndFlush(buildMatch(candidateId, jobId));

        Page<Match> page = matchRepository.findByCandidateId(candidateId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getJobId()).isEqualTo(jobId);
    }

    @Test
    void existsByCandidateIdAndJobId_returnsTrue_whenMatchExists() {
        matchRepository.saveAndFlush(buildMatch(candidateId, jobId));

        assertThat(matchRepository.existsByCandidateIdAndJobId(candidateId, jobId)).isTrue();
    }

    @Test
    void save_throwsDataIntegrityViolationException_whenDuplicateCandidateAndJob() {
        matchRepository.saveAndFlush(buildMatch(candidateId, jobId));

        assertThatThrownBy(() ->
                matchRepository.saveAndFlush(buildMatch(candidateId, jobId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
