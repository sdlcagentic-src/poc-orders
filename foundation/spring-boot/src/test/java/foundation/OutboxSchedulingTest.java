package foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import foundation.messaging.EventSink;
import foundation.outbox.Outbox;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** With a short poll interval the relay runs by itself, on a database of its own: an event written in a transaction reaches the broker without anyone calling the relay. */
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        // its own database, and polling from the start: this context stays alive for the rest of the test run and must not take the rows of other tests
        "spring.datasource.url=jdbc:h2:mem:scheduling;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "foundation.outbox.initial-delay-ms=0",
        "foundation.outbox.poll-interval-ms=50"})
@Import(OutboxSchedulingTest.Recording.class)
class OutboxSchedulingTest {

    static final List<String> SENT = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class Recording {
        @Bean
        @Primary
        EventSink scheduledSink() {
            return (topic, key, payload) -> SENT.add(topic + "|" + payload);
        }
    }

    @Autowired
    Outbox outbox;
    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void theScheduledRelayDeliversWithoutBeingCalled() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> outbox.publish("t.scheduled", "k", "by itself"));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(SENT).contains("t.scheduled|by itself"));
    }
}
