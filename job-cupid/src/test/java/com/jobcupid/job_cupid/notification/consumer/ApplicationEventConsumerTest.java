package com.jobcupid.job_cupid.notification.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
class ApplicationEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock JobRepository       jobRepository;

    ApplicationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ApplicationEventConsumer(notificationService, new ObjectMapper(), jobRepository);
    }

    @Test
    void consume_createsCandidateNotification_whenStatusIsAccepted() throws Exception {
        UUID candidateId    = UUID.randomUUID();
        UUID jobId          = UUID.randomUUID();
        UUID applicationId  = UUID.randomUUID();

        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.empty());

        String payload = """
                {
                  "eventId": "%s",
                  "candidateId": "%s",
                  "jobId": "%s",
                  "applicationId": "%s",
                  "status": "ACCEPTED",
                  "timestamp": null
                }""".formatted(UUID.randomUUID(), candidateId, jobId, applicationId);

        consumer.consume(payload);

        verify(notificationService).createNotification(
                eq(candidateId), eq(NotificationType.APPLICATION_STATUS_CHANGED),
                any(), any(), eq(applicationId), eq("APPLICATION"));
    }

    @Test
    void consume_createsEmployerNotification_whenStatusIsPending() throws Exception {
        UUID candidateId   = UUID.randomUUID();
        UUID employerId    = UUID.randomUUID();
        UUID jobId         = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        Job job = Job.builder()
                .id(jobId).employerId(employerId)
                .title("SWE").description("Build").category("Eng")
                .employmentType(EmploymentType.FULL_TIME).build();
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));

        String payload = """
                {
                  "eventId": "%s",
                  "candidateId": "%s",
                  "jobId": "%s",
                  "applicationId": "%s",
                  "status": "PENDING",
                  "timestamp": null
                }""".formatted(UUID.randomUUID(), candidateId, jobId, applicationId);

        consumer.consume(payload);

        verify(notificationService).createNotification(
                eq(employerId), eq(NotificationType.APPLICATION_RECEIVED),
                any(), any(), eq(applicationId), eq("APPLICATION"));
    }

    @Test
    void consume_doesNotNotifyCandidate_whenStatusIsReviewed() throws Exception {
        UUID candidateId = UUID.randomUUID();
        UUID jobId       = UUID.randomUUID();

        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.empty());

        String payload = """
                {
                  "eventId": "%s",
                  "candidateId": "%s",
                  "jobId": "%s",
                  "applicationId": "%s",
                  "status": "REVIEWED",
                  "timestamp": null
                }""".formatted(UUID.randomUUID(), candidateId, jobId, UUID.randomUUID());

        consumer.consume(payload);

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void consume_throwsException_whenServiceFails_allowingDltRouting() throws Exception {
        UUID candidateId   = UUID.randomUUID();
        UUID jobId         = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("DB unavailable"))
                .when(notificationService).createNotification(any(), any(), any(), any(), any(), any());

        String payload = """
                {
                  "eventId": "%s",
                  "candidateId": "%s",
                  "jobId": "%s",
                  "applicationId": "%s",
                  "status": "ACCEPTED",
                  "timestamp": null
                }""".formatted(UUID.randomUUID(), candidateId, jobId, applicationId);

        // Exception propagates — @RetryableTopic intercepts and routes to DLT after retries
        assertThatThrownBy(() -> consumer.consume(payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB unavailable");
    }
}
