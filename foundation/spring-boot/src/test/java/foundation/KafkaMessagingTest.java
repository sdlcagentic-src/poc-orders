package foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import foundation.messaging.EventSink;
import foundation.messaging.EventSinkException;
import foundation.messaging.KafkaEventSink;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

/** Against an in-process Kafka: the sink delivers, and a failing message is retried the configured number of times and then handled as configured. */
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=foundation-test",
        "foundation.messaging.retry-initial-interval-ms=20",
        "foundation.messaging.retry-max-interval-ms=100"})
@EmbeddedKafka(partitions = 1, topics = {"sink.check", "retry.check", "retry.check.DLT"})
@Import(KafkaMessagingTest.Listeners.class)
class KafkaMessagingTest {

    static final List<String> SINK = new CopyOnWriteArrayList<>();
    static final AtomicInteger ATTEMPTS = new AtomicInteger();
    static final List<String> DEAD = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class Listeners {
        @KafkaListener(topics = "sink.check")
        void sink(ConsumerRecord<String, String> record) {
            SINK.add(record.key() + "=" + record.value());
        }

        @KafkaListener(topics = "retry.check")
        void retry(ConsumerRecord<String, String> record) {
            ATTEMPTS.incrementAndGet();
            throw new IllegalStateException("cannot handle " + record.value());
        }

        @KafkaListener(topics = "retry.check.DLT")
        void dead(ConsumerRecord<String, String> record) {
            DEAD.add(record.value());
        }
    }

    @Autowired
    EventSink sink;

    @Test
    void theSinkDeliversAnEventWithItsKey() {
        sink.send("sink.check", "k1", "hello");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(SINK).contains("k1=hello"));
    }

    @Test
    void theSinkThrowsWhenTheBrokerDoesNotAcceptTheEvent() {
        DefaultKafkaProducerFactory<String, String> unreachable = new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:1",
                ProducerConfig.MAX_BLOCK_MS_CONFIG, 300,
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 600,
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 300,
                ProducerConfig.LINGER_MS_CONFIG, 0), new StringSerializer(), new StringSerializer());
        KafkaEventSink sink = new KafkaEventSink(new KafkaTemplate<>(unreachable), Duration.ofSeconds(5));
        assertThatThrownBy(() -> sink.send("sink.check", "k", "lost")).isInstanceOf(EventSinkException.class).hasMessageContaining("sink.check");
    }

    @Test
    void aFailingMessageIsTriedOnceAndThenRetriedTheConfiguredNumberOfTimes() throws Exception {
        sink.send("retry.check", "k2", "poison");
        int expected = 1 + 3;
        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> assertThat(ATTEMPTS.get()).isEqualTo(expected));
        Thread.sleep(1_000);
        assertThat(ATTEMPTS.get()).as("no further attempts after the retries").isEqualTo(expected);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(DEAD).containsExactly("poison"));
    }
}
