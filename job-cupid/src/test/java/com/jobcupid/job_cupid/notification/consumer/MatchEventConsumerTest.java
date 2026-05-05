package com.jobcupid.job_cupid.notification.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobcupid.job_cupid.job.entity.EmploymentType;
import com.jobcupid.job_cupid.job.entity.Job;
import com.jobcupid.job_cupid.job.repository.JobRepository;
import com.jobcupid.job_cupid.notification.entity.NotificationType;
import com.jobcupid.job_cupid.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class MatchEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock JobRepository       jobRepository;

    MatchEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new MatchEventConsumer(notificationService, new ObjectMapper(), jobRepository);
    }

    @Test
    void consume_creates2Notifications_whenMatchEventReceived() throws Exception {
        UUID candidateId = UUID.randomUUID();
        UUID employerId  = UUID.randomUUID();
        UUID jobId       = UUID.randomUUID();
        UUID matchId     = UUID.randomUUID();

        Job job = Job.builder()
                .id(jobId).employerId(employerId)
                .title("Senior SWE").description("Build stuff").category("Eng")
                .employmentType(EmploymentType.FULL_TIME).build();

        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));

        String payload = """
                {
                  "eventId": "%s",
                  "candidateId": "%s",
                  "employerId": "%s",
                  "jobId": "%s",
                  "matchId": "%s",
                  "timestamp": null
                }""".formatted(UUID.randomUUID(), candidateId, employerId, jobId, matchId);

        consumer.consume(payload);

        verify(notificationService, times(2))
                .createNotification(any(), any(), any(), any(), any(), any());
        verify(notificationService).createNotification(
                eq(candidateId), eq(NotificationType.MATCH_CREATED),
                any(), any(), eq(matchId), eq("MATCH"));
        verify(notificationService).createNotification(
                eq(employerId), eq(NotificationType.MATCH_CREATED),
                any(), any(), eq(matchId), eq("MATCH"));
    }

    @Test
    void consume_stillCreates2Notifications_whenJobNotFound() throws Exception {
        UUID candidateId = UUID.randomUUID();
        UUID employerId  = UUID.randomUUID();
        UUID jobId       = UUID.randomUUID();
        UUID matchId     = UUID.randomUUID();

        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.empty());

        String payload = """
                {
                  "eventId": "%s",
                  "candidateId": "%s",
                  "employerId": "%s",
                  "jobId": "%s",
                  "matchId": "%s",
                  "timestamp": null
                }""".formatted(UUID.randomUUID(), candidateId, employerId, jobId, matchId);

        consumer.consume(payload);

        verify(notificationService, times(2))
                .createNotification(any(), any(), any(), any(), any(), any());
    }
}
