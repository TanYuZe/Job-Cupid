package com.jobcupid.job_cupid.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.ActiveProfiles;

import com.jobcupid.job_cupid.user.entity.CandidateProfile;

/**
 * Repository slice test — hits the real PostgreSQL (dev profile).
 * Requires: docker-compose up -d postgres (or native PostgreSQL on localhost:5432).
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class CandidateProfileRepositoryTest {

    @Autowired
    CandidateProfileRepository repository;

    @Test
    void findByUserId_returnsProfile_whenExists() {
        UUID userId = UUID.randomUUID();
        CandidateProfile saved = repository.save(
                CandidateProfile.builder()
                        .userId(userId)
                        .headline("Java Developer")
                        .build());

        Optional<CandidateProfile> result = repository.findByUserId(userId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getHeadline()).isEqualTo("Java Developer");
    }

    @Test
    void findByUserId_returnsEmpty_whenNotExists() {
        Optional<CandidateProfile> result = repository.findByUserId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void skills_areRetrievableAsList_afterSave() {
        UUID userId = UUID.randomUUID();
        List<String> skills = List.of("Java", "Spring Boot", "PostgreSQL");

        repository.save(CandidateProfile.builder()
                .userId(userId)
                .skills(skills)
                .build());

        CandidateProfile loaded = repository.findByUserId(userId).orElseThrow();
        assertThat(loaded.getSkills()).containsExactlyInAnyOrderElementsOf(skills);
    }
}
