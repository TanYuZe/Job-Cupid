package com.jobcupid.job_cupid.job.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobcupid.job_cupid.job.entity.EmploymentType;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;
import com.jobcupid.job_cupid.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class BoostScoreSchedulerTest {

    @Mock UserRepository userRepository;
    @Mock JobRepository  jobRepository;

    @InjectMocks BoostScoreScheduler scheduler;

    private User premiumEmployer(UUID id) {
        return User.builder()
                .id(id).email("employer-" + id + "@test.com")
                .passwordHash("x").firstName("Bob").lastName("Smith")
                .role(UserRole.EMPLOYER).isPremium(true).isActive(true).isBanned(false)
                .build();
    }

    private Job jobFor(UUID employerId, int currentBoost) {
        return Job.builder()
                .id(UUID.randomUUID()).employerId(employerId)
                .title("SWE").description("Build stuff").category("Eng")
                .employmentType(EmploymentType.FULL_TIME)
                .boostScore(currentBoost)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void resetBoostScores_setsZero_forNonPremiumEmployerJobs() {
        UUID nonPremiumEmployerId = UUID.randomUUID();
        Job job = jobFor(nonPremiumEmployerId, 100);

        when(userRepository.findByIsPremiumAndRoleAndDeletedAtIsNull(true, UserRole.EMPLOYER))
                .thenReturn(List.of());
        when(jobRepository.findByDeletedAtIsNull()).thenReturn(List.of(job));

        scheduler.resetBoostScores();

        ArgumentCaptor<List<Job>> captor = ArgumentCaptor.forClass(List.class);
        verify(jobRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getBoostScore()).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void resetBoostScores_setsOneHundred_forPremiumEmployerJobs() {
        UUID premiumEmployerId = UUID.randomUUID();
        User employer = premiumEmployer(premiumEmployerId);
        Job job = jobFor(premiumEmployerId, 0);

        when(userRepository.findByIsPremiumAndRoleAndDeletedAtIsNull(true, UserRole.EMPLOYER))
                .thenReturn(List.of(employer));
        when(jobRepository.findByDeletedAtIsNull()).thenReturn(List.of(job));

        scheduler.resetBoostScores();

        ArgumentCaptor<List<Job>> captor = ArgumentCaptor.forClass(List.class);
        verify(jobRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getBoostScore()).isEqualTo(100);
    }
}
