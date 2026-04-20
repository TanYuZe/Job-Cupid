package com.jobcupid.job_cupid.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.ActiveProfiles;

import com.jobcupid.job_cupid.user.entity.EmployerProfile;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class EmployerProfileRepositoryTest {

    @Autowired
    EmployerProfileRepository employerProfileRepository;

    @Autowired
    UserRepository userRepository;

    private User savedUser() {
        return userRepository.save(User.builder()
                .email(UUID.randomUUID() + "@test.com")
                .passwordHash("$2a$12$hashed")
                .firstName("Employer")
                .lastName("Test")
                .role(UserRole.EMPLOYER)
                .build());
    }

    @Test
    void findByUserId_returnsProfile_whenExists() {
        User user = savedUser();
        EmployerProfile saved = employerProfileRepository.save(
                EmployerProfile.builder()
                        .userId(user.getId())
                        .companyName("Acme Corp")
                        .industry("Technology")
                        .build());

        Optional<EmployerProfile> result = employerProfileRepository.findByUserId(user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getCompanyName()).isEqualTo("Acme Corp");
        assertThat(result.get().getIndustry()).isEqualTo("Technology");
    }

    @Test
    void findByUserId_returnsEmpty_whenNotExists() {
        Optional<EmployerProfile> result = employerProfileRepository.findByUserId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void allFields_arePersistedAndRetrievable() {
        User user = savedUser();
        employerProfileRepository.save(EmployerProfile.builder()
                .userId(user.getId())
                .companyName("StartupXYZ")
                .companyDescription("We build things")
                .companyWebsite("https://startupxyz.com")
                .companyLogoUrl("https://cdn.startupxyz.com/logo.png")
                .companySize("11-50")
                .industry("Fintech")
                .foundedYear((short) 2020)
                .build());

        EmployerProfile loaded = employerProfileRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(loaded.getCompanyDescription()).isEqualTo("We build things");
        assertThat(loaded.getCompanySize()).isEqualTo("11-50");
        assertThat(loaded.getFoundedYear()).isEqualTo((short) 2020);
    }
}
