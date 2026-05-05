package com.jobcupid.job_cupid.match.consumer;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobcupid.job_cupid.match.service.MatchService;

@ExtendWith(MockitoExtension.class)
class ApplicationEventConsumerTest {

    @Mock MatchService matchService;

    ApplicationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ApplicationEventConsumer(matchService, new ObjectMapper());
    }

    @Test
    void consume_callsEvaluateMatchGate_whenStatusIsPending() {
        UUID candidateId   = UUID.randomUUID();
        UUID jobId         = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        String payload = """
                {
                  "eventId": "%s",
                  "candidateId": "%s",
                  "jobId": "%s",
                  "applicationId": "%s",
                  "status": "PENDING",
                  "timestamp": "2024-01-01T00:00:00Z"
                }""".formatted(UUID.randomUUID(), candidateId, jobId, applicationId);

        consumer.consume(payload);

        verify(matchService).evaluateMatchGate(candidateId, jobId);
    }

    @Test
    void consume_doesNotCallEvaluateMatchGate_whenStatusIsReviewed() {
        String payload = """
                {
                  "eventId": "%s",
                  "candidateId": "%s",
                  "jobId": "%s",
                  "applicationId": "%s",
                  "status": "REVIEWED",
                  "timestamp": "2024-01-01T00:00:00Z"
                }""".formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        consumer.consume(payload);

        verify(matchService, never()).evaluateMatchGate(ArgumentMatchers.any(), ArgumentMatchers.any());
    }
}
