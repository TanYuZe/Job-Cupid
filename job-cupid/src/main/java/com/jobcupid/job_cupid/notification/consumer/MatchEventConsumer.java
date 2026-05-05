package com.jobcupid.job_cupid.notification.consumer;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.match.event.MatchEvent;
import com.jobcupid.job_cupid.notification.entity.NotificationType;
import com.jobcupid.job_cupid.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MatchEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MatchEventConsumer.class);

    private final NotificationService notificationService;
    private final ObjectMapper        objectMapper;
    private final JobRepository       jobRepository;

    @RetryableTopic(attempts = "3")
    @KafkaListener(topics = "match.events", groupId = "notification-service")
    public void consume(String payload) throws Exception {
        MatchEvent event = objectMapper.readValue(payload, MatchEvent.class);

        Optional<Job> jobOpt = jobRepository.findByIdAndDeletedAtIsNull(event.jobId());
        String jobTitle = jobOpt.map(Job::getTitle).orElse("a position");

        log.debug("Creating match notifications for candidateId={} employerId={}",
                event.candidateId(), event.employerId());

        notificationService.createNotification(
                event.candidateId(),
                NotificationType.MATCH_CREATED,
                "You have a new match!",
                "You matched with a company for " + jobTitle,
                event.matchId(),
                "MATCH");

        notificationService.createNotification(
                event.employerId(),
                NotificationType.MATCH_CREATED,
                "New match for " + jobTitle + "!",
                "A candidate matched your job posting.",
                event.matchId(),
                "MATCH");
    }
}
