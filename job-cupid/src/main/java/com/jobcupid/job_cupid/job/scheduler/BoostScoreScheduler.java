package com.jobcupid.job_cupid.job.scheduler;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.user.entity.UserRole;
import com.jobcupid.job_cupid.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BoostScoreScheduler {

    private static final Logger log = LoggerFactory.getLogger(BoostScoreScheduler.class);

    private final UserRepository userRepository;
    private final JobRepository  jobRepository;

    @Scheduled(cron = "0 5 1 * * *")
    @Transactional
    public void resetBoostScores() {
        Set<UUID> premiumEmployerIds = userRepository
                .findByIsPremiumAndRoleAndDeletedAtIsNull(true, UserRole.EMPLOYER)
                .stream().map(u -> u.getId()).collect(Collectors.toSet());

        List<Job> jobs = jobRepository.findByDeletedAtIsNull();

        int premiumCount = 0;
        int nonPremiumCount = 0;
        for (Job job : jobs) {
            if (premiumEmployerIds.contains(job.getEmployerId())) {
                job.setBoostScore(100);
                premiumCount++;
            } else {
                job.setBoostScore(0);
                nonPremiumCount++;
            }
        }

        jobRepository.saveAll(jobs);
        log.info("Boost score reset: {} job(s) set to 100 (premium employers), {} job(s) reset to 0",
                premiumCount, nonPremiumCount);
    }
}
