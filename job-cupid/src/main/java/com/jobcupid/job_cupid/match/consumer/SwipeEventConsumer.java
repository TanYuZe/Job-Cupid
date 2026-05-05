package com.jobcupid.job_cupid.match.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobcupid.job_cupid.application.repository.ApplicationRepository;
import com.jobcupid.job_cupid.match.service.MatchService;
import com.jobcupid.job_cupid.swipe.event.SwipeEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SwipeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SwipeEventConsumer.class);

    private final MatchService          matchService;
    private final ObjectMapper          objectMapper;
    private final ApplicationRepository applicationRepository;

    @KafkaListener(topics = "swipe.events", groupId = "match-service")
    public void consume(String payload) {
        try {
            SwipeEvent event = objectMapper.readValue(payload, SwipeEvent.class);
            if ("EMPLOYER".equals(event.actorRole()) && "LIKE".equals(event.action())) {
                applicationRepository.findById(event.targetId())
                        .ifPresent(app ->
                                matchService.evaluateMatchGate(app.getCandidateId(), app.getJobId()));
            }
        } catch (Exception e) {
            log.error("Failed to process SwipeEvent payload: {}", e.getMessage(), e);
        }
    }
}
