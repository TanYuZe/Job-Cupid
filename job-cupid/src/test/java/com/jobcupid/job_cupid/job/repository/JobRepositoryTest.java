package com.jobcupid.job_cupid.job.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.jobcupid.job_cupid.job.entity.EmploymentType;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;
import com.jobcupid.job_cupid.user.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class JobRepositoryTest {

    @Autowired JobRepository jobRepository;
    @Autowired UserRepository userRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User savedEmployer() {
        return userRepository.save(User.builder()
                .email(UUID.randomUUID() + "@employer.com")
                .passwordHash("$2a$12$hashed")
                .firstName("Employer")
                .lastName("Test")
                .role(UserRole.EMPLOYER)
                .isPremium(false)
                .isActive(true)
                .isBanned(false)
                .build());
    }

    private Job buildJob(UUID employerId) {
        return Job.builder()
                .employerId(employerId)
                .title("Software Engineer")
                .description("Build great things")
                .category("Engineering")
                .employmentType(EmploymentType.FULL_TIME)
                .requiredSkills(List.of("Java", "Spring Boot"))
                .build();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void findByIdAndDeletedAtIsNull_returnsJob_whenActive() {
        User employer = savedEmployer();
        Job saved = jobRepository.save(buildJob(employer.getId()));

        Optional<Job> result = jobRepository.findByIdAndDeletedAtIsNull(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Software Engineer");
        assertThat(result.get().getRequiredSkills()).containsExactly("Java", "Spring Boot");
    }

    @Test
    void findByIdAndDeletedAtIsNull_returnsEmpty_whenSoftDeleted() {
        User employer = savedEmployer();
        Job job = jobRepository.save(buildJob(employer.getId()));
        job.setDeletedAt(OffsetDateTime.now());
        jobRepository.save(job);

        Optional<Job> result = jobRepository.findByIdAndDeletedAtIsNull(job.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmployerIdAndDeletedAtIsNull_returnsPageOf3_forEmployerWith3Jobs() {
        User employer = savedEmployer();
        jobRepository.save(buildJob(employer.getId()));
        jobRepository.save(buildJob(employer.getId()));
        jobRepository.save(buildJob(employer.getId()));

        Page<Job> result = jobRepository.findByEmployerIdAndDeletedAtIsNull(
                employer.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).allMatch(j -> j.getDeletedAt() == null);
    }
}
