package foundation.messaging;

import java.time.Duration;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Kafka for the service: the {@link EventSink}, and the error handling of every {@code @KafkaListener}. A failed message is retried
 * 3 times by default ({@code foundation.messaging.retry-attempts}) with exponential backoff
 * and then published to the topic named {@code <topic>.DLT} (the name is fixed here, not left to the library's default, which has changed between
 * versions; the producer chooses the partition).
 */
@Configuration(proxyBeanMethods = false)
public class MessagingConfiguration {

    @Bean
    EventSink kafkaEventSink(KafkaTemplate<String, String> template, @Value("${foundation.messaging.send-timeout-ms:10000}") long timeoutMs) {
        return new KafkaEventSink(template, Duration.ofMillis(timeoutMs));
    }

    @Bean
    DefaultErrorHandler foundationKafkaErrorHandler(KafkaOperations<Object, Object> operations,
                                                    @Value("${foundation.messaging.retry-attempts:3}") int retryAttempts,
                                                    @Value("${foundation.messaging.retry-initial-interval-ms:1000}") long initialIntervalMs,
                                                    @Value("${foundation.messaging.retry-max-interval-ms:30000}") long maxIntervalMs) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(retryAttempts);
        backOff.setInitialInterval(initialIntervalMs);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(maxIntervalMs);
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(operations, (record, e) -> new TopicPartition(record.topic() + ".DLT", -1));
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
