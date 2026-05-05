package com.jobcupid.job_cupid.match.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MatchEventPublisher {

    private static final Logger log   = LoggerFactory.getLogger(MatchEventPublisher.class);
    private static final String TOPIC = "match.events";

    private final KafkaTemplate<String, MatchEvent> matchKafkaTemplate;

    public void publish(MatchEvent event) {
        matchKafkaTemplate.send(TOPIC, event.eventId(), event)
                .exceptionally(ex -> {
                    log.error("Failed to publish MatchEvent [eventId={}]: {}",
                            event.eventId(), ex.getMessage());
                    return null;
                });
    }
}
