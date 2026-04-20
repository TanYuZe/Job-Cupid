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
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;

/**
 * Repository slice test — hits the real PostgreSQL (dev profile).
 * Requires native PostgreSQL on localhost:5432 (or docker-compose up -d postgres).
 * Each test creates its own User row to satisfy the FK constraint.
 */
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class CandidateProfileRepositoryTest {

    @Autowired
    CandidateProfileRepository candidateProfileRepository;

    @Autowired
    UserRepository userRepository;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User savedUser() {
        return userRepository.save(User.builder()
                .email(UUID.randomUUID() + "@test.com")
                .passwordHash("$2a$12$hashed")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .build());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void findByUserId_returnsProfile_whenExists() {
        User user = savedUser();
        CandidateProfile saved = candidateProfileRepository.save(
                CandidateProfile.builder()
                        .userId(user.getId())
                        .headline("Java Developer")
                        .build());

        Optional<CandidateProfile> result = candidateProfileRepository.findByUserId(user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getHeadline()).isEqualTo("Java Developer");
    }

    @Test
    void findByUserId_returnsEmpty_whenNotExists() {
        Optional<CandidateProfile> result = candidateProfileRepository.findByUserId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void skills_areRetrievableAsList_afterSave() {
        User user = savedUser();
        List<String> skills = List.of("Java", "Spring Boot", "PostgreSQL");

        candidateProfileRepository.save(CandidateProfile.builder()
                .userId(user.getId())
                .skills(skills)
                .build());

        CandidateProfile loaded = candidateProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(loaded.getSkills()).containsExactlyInAnyOrderElementsOf(skills);
    }
}
