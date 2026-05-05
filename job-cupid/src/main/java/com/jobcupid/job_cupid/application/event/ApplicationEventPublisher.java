package com.jobcupid.job_cupid.application.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApplicationEventPublisher {

    private static final Logger log   = LoggerFactory.getLogger(ApplicationEventPublisher.class);
    private static final String TOPIC = "application.events";

    private final KafkaTemplate<String, ApplicationEvent> kafkaTemplate;

    public void publish(ApplicationEvent event) {
        kafkaTemplate.send(TOPIC, event.eventId(), event)
                .exceptionally(ex -> {
                    log.error("Failed to publish ApplicationEvent [eventId={}]: {}",
                            event.eventId(), ex.getMessage());
                    return null;
                });
    }
}
