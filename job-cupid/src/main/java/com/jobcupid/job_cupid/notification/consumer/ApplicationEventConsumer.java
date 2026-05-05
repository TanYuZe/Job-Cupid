package com.jobcupid.job_cupid.notification.consumer;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobcupid.job_cupid.application.event.ApplicationEvent;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.notification.entity.NotificationType;
import com.jobcupid.job_cupid.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApplicationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ApplicationEventConsumer.class);

    private final NotificationService notificationService;
    private final ObjectMapper        objectMapper;
    private final JobRepository       jobRepository;

    @RetryableTopic(attempts = "3")
    @KafkaListener(topics = "application.events", groupId = "notification-service")
    public void consume(String payload) throws Exception {
        ApplicationEvent event = objectMapper.readValue(payload, ApplicationEvent.class);

        Optional<Job> jobOpt = jobRepository.findByIdAndDeletedAtIsNull(event.jobId());
        String jobTitle = jobOpt.map(Job::getTitle).orElse("a position");

        switch (event.status()) {
            case "PENDING" -> {
                jobOpt.ifPresent(job -> {
                    log.debug("Notifying employer {} of new application for job {}",
                            job.getEmployerId(), event.jobId());
                    notificationService.createNotification(
                            job.getEmployerId(),
                            NotificationType.APPLICATION_RECEIVED,
                            "New application received",
                            "New application received for " + jobTitle,
                            event.applicationId(),
                            "APPLICATION");
                });
            }
            case "ACCEPTED" -> {
                log.debug("Notifying candidate {} of accepted application", event.candidateId());
                notificationService.createNotification(
                        event.candidateId(),
                        NotificationType.APPLICATION_STATUS_CHANGED,
                        "Your application was accepted!",
                        "Congratulations! Your application for " + jobTitle + " was accepted.",
                        event.applicationId(),
                        "APPLICATION");
            }
            case "REJECTED" -> {
                log.debug("Notifying candidate {} of rejected application", event.candidateId());
                notificationService.createNotification(
                        event.candidateId(),
                        NotificationType.APPLICATION_STATUS_CHANGED,
                        "Application update",
                        "Your application for " + jobTitle + " was not selected.",
                        event.applicationId(),
                        "APPLICATION");
            }
            default -> log.debug("No notification configured for application status: {}", event.status());
        }
    }
}
