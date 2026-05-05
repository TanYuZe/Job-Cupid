package com.jobcupid.job_cupid.swipe.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.jobcupid.job_cupid.shared.config.KafkaConfig;

@SpringJUnitConfig(classes = {KafkaConfig.class, SwipeEventPublisher.class})
@EmbeddedKafka(partitions = 1, topics = "swipe.events", bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class SwipeEventPublisherTest {

    @Autowired SwipeEventPublisher publisher;
    @Autowired EmbeddedKafkaBroker embeddedKafka;

    private SwipeEvent buildEvent() {
        return new SwipeEvent(
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                "CANDIDATE",
                UUID.randomUUID(),
                "JOB",
                "LIKE",
                Instant.now()
        );
    }

    @Test
    void publish_sendsMessageToSwipeEventsTopic() {
        SwipeEvent event = buildEvent();
        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-group",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true"
        );
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            consumer.subscribe(java.util.List.of("swipe.events"));

            publisher.publish(event);

            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
            assertThat(records).isNotEmpty();
            assertThat(records.iterator().next().key()).isEqualTo(event.eventId());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void publish_catchesAndLogsException_whenKafkaThrows() {
        KafkaTemplate<String, SwipeEvent> failingTemplate = mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, SwipeEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka unreachable"));
        when(failingTemplate.send(anyString(), anyString(), any(SwipeEvent.class)))
                .thenReturn(failedFuture);

        SwipeEventPublisher failingPublisher = new SwipeEventPublisher(failingTemplate);

        assertThatNoException().isThrownBy(() -> failingPublisher.publish(buildEvent()));
    }
}
