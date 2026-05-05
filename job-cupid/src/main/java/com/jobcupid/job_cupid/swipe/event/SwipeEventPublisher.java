package com.jobcupid.job_cupid.swipe.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SwipeEventPublisher {

    private static final String TOPIC = "swipe.events";

    private final KafkaTemplate<String, SwipeEvent> kafkaTemplate;

    public void publish(SwipeEvent event) {
        kafkaTemplate.send(TOPIC, event.eventId(), event)
                .exceptionally(ex -> {
                    log.error("Failed to publish SwipeEvent eventId={}: {}", event.eventId(), ex.getMessage());
                    return null;
                });
    }
}
