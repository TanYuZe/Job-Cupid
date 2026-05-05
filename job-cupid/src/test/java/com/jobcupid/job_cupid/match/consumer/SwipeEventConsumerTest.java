package com.jobcupid.job_cupid.match.consumer;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobcupid.job_cupid.application.entity.Application;
import com.jobcupid.job_cupid.application.entity.ApplicationStatus;
import com.jobcupid.job_cupid.application.repository.ApplicationRepository;
import com.jobcupid.job_cupid.match.service.MatchService;

@ExtendWith(MockitoExtension.class)
class SwipeEventConsumerTest {

    @Mock MatchService          matchService;
    @Mock ApplicationRepository applicationRepository;

    SwipeEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SwipeEventConsumer(matchService, new ObjectMapper(), applicationRepository);
    }

    @Test
    void consume_callsEvaluateMatchGate_whenEmployerLikesApplication() throws Exception {
        UUID candidateId   = UUID.randomUUID();
        UUID jobId         = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        Application app = Application.builder()
                .id(applicationId).candidateId(candidateId).jobId(jobId)
                .status(ApplicationStatus.PENDING).build();

        String payload = """
                {
                  "eventId": "%s",
                  "actorId": "%s",
                  "actorRole": "EMPLOYER",
                  "targetId": "%s",
                  "targetType": "APPLICATION",
                  "action": "LIKE",
                  "timestamp": "2024-01-01T00:00:00Z"
                }""".formatted(UUID.randomUUID(), UUID.randomUUID(), applicationId);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(app));

        consumer.consume(payload);

        verify(matchService).evaluateMatchGate(candidateId, jobId);
    }

    @Test
    void consume_doesNotCallEvaluateMatchGate_whenCandidateSwipes() {
        UUID targetId = UUID.randomUUID();

        String payload = """
                {
                  "eventId": "%s",
                  "actorId": "%s",
                  "actorRole": "CANDIDATE",
                  "targetId": "%s",
                  "targetType": "JOB",
                  "action": "LIKE",
                  "timestamp": "2024-01-01T00:00:00Z"
                }""".formatted(UUID.randomUUID(), UUID.randomUUID(), targetId);

        consumer.consume(payload);

        verify(matchService, never()).evaluateMatchGate(ArgumentMatchers.any(), ArgumentMatchers.any());
        verify(applicationRepository, never()).findById(targetId);
    }

    @Test
    void consume_doesNotCallEvaluateMatchGate_whenEmployerPasses() {
        String payload = """
                {
                  "eventId": "%s",
                  "actorId": "%s",
                  "actorRole": "EMPLOYER",
                  "targetId": "%s",
                  "targetType": "APPLICATION",
                  "action": "PASS",
                  "timestamp": "2024-01-01T00:00:00Z"
                }""".formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        consumer.consume(payload);

        verify(matchService, never()).evaluateMatchGate(ArgumentMatchers.any(), ArgumentMatchers.any());
    }
}
