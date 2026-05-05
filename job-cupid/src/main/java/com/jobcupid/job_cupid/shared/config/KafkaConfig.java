package com.jobcupid.job_cupid.shared.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.jobcupid.job_cupid.application.event.ApplicationEvent;
import com.jobcupid.job_cupid.match.event.MatchEvent;
import com.jobcupid.job_cupid.swipe.event.SwipeEvent;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Autowired
    private ObjectMapper objectMapper;

    // ── Swipe events ──────────────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, SwipeEvent> swipeEventProducerFactory() {
        return buildProducerFactory();
    }

    @Bean
    public KafkaTemplate<String, SwipeEvent> kafkaTemplate(
            ProducerFactory<String, SwipeEvent> swipeEventProducerFactory) {
        return new KafkaTemplate<>(swipeEventProducerFactory);
    }

    @Bean
    public NewTopic swipeEventsTopic() {
        return TopicBuilder.name("swipe.events").partitions(3).replicas(1).build();
    }

    // ── Application events ────────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, ApplicationEvent> applicationEventProducerFactory() {
        return buildProducerFactory();
    }

    @Bean
    public KafkaTemplate<String, ApplicationEvent> applicationKafkaTemplate(
            ProducerFactory<String, ApplicationEvent> applicationEventProducerFactory) {
        return new KafkaTemplate<>(applicationEventProducerFactory);
    }

    @Bean
    public NewTopic applicationEventsTopic() {
        return TopicBuilder.name("application.events").partitions(3).replicas(1).build();
    }

    // ── Match events ──────────────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, MatchEvent> matchEventProducerFactory() {
        return buildProducerFactory();
    }

    @Bean
    public KafkaTemplate<String, MatchEvent> matchKafkaTemplate(
            ProducerFactory<String, MatchEvent> matchEventProducerFactory) {
        return new KafkaTemplate<>(matchEventProducerFactory);
    }

    @Bean
    public NewTopic matchEventsTopic() {
        return TopicBuilder.name("match.events").partitions(3).replicas(1).build();
    }

    // ── Shared producer config ────────────────────────────────────────────────

    private <V> ProducerFactory<String, V> buildProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        Serializer<V> valueSerializer = (topic, data) -> {
            try {
                return objectMapper.writeValueAsBytes(data);
            } catch (JsonProcessingException e) {
                throw new SerializationException("Error serializing Kafka message for topic " + topic, e);
            }
        };

        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), valueSerializer);
    }
}
