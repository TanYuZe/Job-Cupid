package com.jobcupid.job_cupid.match.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobcupid.job_cupid.application.event.ApplicationEvent;
import com.jobcupid.job_cupid.match.service.MatchService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApplicationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ApplicationEventConsumer.class);

    private final MatchService matchService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "application.events", groupId = "match-service")
    public void consume(String payload) {
        try {
            ApplicationEvent event = objectMapper.readValue(payload, ApplicationEvent.class);
            if ("PENDING".equals(event.status())) {
                matchService.evaluateMatchGate(event.candidateId(), event.jobId());
            }
        } catch (Exception e) {
            log.error("Failed to process ApplicationEvent payload: {}", e.getMessage(), e);
        }
    }
}
