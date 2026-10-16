package foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import foundation.messaging.EventSink;
import foundation.messaging.EventSinkException;
import foundation.outbox.Outbox;
import foundation.outbox.OutboxCleaner;
import foundation.outbox.OutboxRelay;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(OutboxTest.Recording.class)
class OutboxTest {

    /** Stands in for the broker: keeps what it is given and can be told to fail. */
    static class RecordingSink implements EventSink {
        final List<String> sent = new CopyOnWriteArrayList<>();
        volatile boolean failing;

        @Override
        public void send(String topic, String key, String payload) {
            if (failing) {
                throw new EventSinkException("broker down", null);
            }
            sent.add(topic + "|" + key + "|" + payload);
        }
    }

    @TestConfiguration
    static class Recording {
        @Bean
        @Primary
        RecordingSink recordingSink() {
            return new RecordingSink();
        }
    }

    @Autowired
    Outbox outbox;
    @Autowired
    OutboxRelay relay;
    @Autowired
    OutboxCleaner cleaner;
    @Autowired
    RecordingSink sink;
    @Autowired
    JdbcClient jdbc;
    @Autowired
    PlatformTransactionManager transactionManager;

    private TransactionTemplate tx() {
        return new TransactionTemplate(transactionManager);
    }

    private int count(String where) {
        return jdbc.sql("SELECT COUNT(*) FROM outbox_event WHERE " + where).query(Integer.class).single();
    }

    @BeforeEach
    void clean() {
        jdbc.sql("DELETE FROM outbox_event").update();
        sink.sent.clear();
        sink.failing = false;
    }

    @Test
    void anEventExistsExactlyWhenTheTransactionCommits() {
        tx().executeWithoutResult(status -> outbox.publish("things.created", "k1", "{\"id\":1}"));
        assertThat(count("1=1")).isEqualTo(1);

        assertThatThrownBy(() -> tx().executeWithoutResult(status -> {
            outbox.publish("things.created", "k2", "{\"id\":2}");
            throw new IllegalStateException("the state change failed");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(count("1=1")).as("a rolled back event must not exist").isEqualTo(1);
    }

    @Test
    void publishingOutsideATransactionIsRefused() {
        assertThatThrownBy(() -> outbox.publish("things.created", "k", "x")).isInstanceOf(IllegalStateException.class).hasMessageContaining("inside the transaction");
    }

    @Test
    void badInputIsRefused() {
        for (String topic : new String[] {null, "", "has space", "a/b", "x".repeat(250)}) {
            assertThatThrownBy(() -> tx().executeWithoutResult(s -> outbox.publish(topic, "k", "x"))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> tx().executeWithoutResult(s -> outbox.publish("t", "k", null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tx().executeWithoutResult(s -> outbox.publish("t", "k".repeat(256), "x"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(count("1=1")).isZero();
    }

    @Test
    void theRelaySendsInOrderMarksThemSentAndSendsEachOnlyOnce() {
        tx().executeWithoutResult(status -> {
            outbox.publish("t.one", "a", "first");
            outbox.publish("t.two", null, "second");
        });
        assertThat(relay.relayOnce()).isEqualTo(2);
        assertThat(sink.sent).containsExactly("t.one|a|first", "t.two|null|second");
        assertThat(count("published_at IS NULL")).isZero();
        assertThat(relay.relayOnce()).as("nothing is sent twice").isZero();
        assertThat(sink.sent).hasSize(2);
    }

    @Test
    void aFailedSendLeavesTheEventForTheNextAttempt() {
        tx().executeWithoutResult(status -> outbox.publish("t.one", "a", "first"));
        sink.failing = true;
        assertThat(relay.relayOnce()).isZero();
        assertThat(count("published_at IS NULL")).isEqualTo(1);
        sink.failing = false;
        assertThat(relay.relayOnce()).isEqualTo(1);
        assertThat(sink.sent).containsExactly("t.one|a|first");
    }

    @Test
    void aBatchStopsAtTheFirstFailureAndKeepsWhatWasAlreadySent() {
        tx().executeWithoutResult(status -> {
            outbox.publish("t.one", "a", "first");
            outbox.publish("t.two", "b", "second");
            outbox.publish("t.three", "c", "third");
        });
        EventSink failOnSecond = new EventSink() {
            @Override
            public void send(String topic, String key, String payload) {
                if (topic.equals("t.two")) {
                    throw new EventSinkException("no", null);
                }
                sink.send(topic, key, payload);
            }
        };
        OutboxRelay partial = new OutboxRelay(jdbc, failOnSecond, tx(), 100);
        assertThat(partial.relayOnce()).isEqualTo(1);
        assertThat(count("published_at IS NOT NULL")).isEqualTo(1);
        assertThat(relay.relayOnce()).isEqualTo(2);
        assertThat(sink.sent).containsExactly("t.one|a|first", "t.two|b|second", "t.three|c|third");
    }

    @Test
    void theBatchSizeLimitsWhatOneRoundSends() {
        tx().executeWithoutResult(status -> {
            for (int i = 0; i < 5; i++) {
                outbox.publish("t.batch", "k" + i, "p" + i);
            }
        });
        OutboxRelay small = new OutboxRelay(jdbc, sink, tx(), 2);
        assertThat(small.relayOnce()).isEqualTo(2);
        assertThat(small.relayOnce()).isEqualTo(2);
        assertThat(small.relayOnce()).isEqualTo(1);
    }

    @Test
    void rowsLockedByAnotherRelayAreSkippedNotSentTwice() throws Exception {
        tx().executeWithoutResult(status -> outbox.publish("t.lock", "a", "only"));
        List<Integer> results = new ArrayList<>();
        // the first relay is inside its transaction (row locked) while a second one runs
        EventSink slow = (topic, key, payload) -> {
            TransactionTemplate separate = new TransactionTemplate(transactionManager);
            separate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            results.add(new OutboxRelay(jdbc, sink, separate, 100).relayOnce());
            sink.send(topic, key, payload);
        };
        assertThat(new OutboxRelay(jdbc, slow, tx(), 100).relayOnce()).isEqualTo(1);
        assertThat(results).containsExactly(0);
        assertThat(sink.sent).containsExactly("t.lock|a|only");
    }

    @Test
    void theCleanerDeletesOnlyOldSentEvents() {
        tx().executeWithoutResult(status -> {
            outbox.publish("t.old", "a", "sent and old");
            outbox.publish("t.new", "b", "sent and new");
            outbox.publish("t.unsent", "c", "never sent");
        });
        sink.failing = false;
        relay.relayOnce();
        jdbc.sql("UPDATE outbox_event SET published_at = :old WHERE topic = 't.old'").param("old", OffsetDateTime.now(ZoneOffset.UTC).minusDays(30)).update();
        jdbc.sql("UPDATE outbox_event SET published_at = NULL WHERE topic = 't.unsent'").update();
        jdbc.sql("UPDATE outbox_event SET created_at = :old WHERE topic = 't.unsent'").param("old", OffsetDateTime.now(ZoneOffset.UTC).minusDays(60)).update();

        assertThat(cleaner.cleanOlderThan(OffsetDateTime.now(ZoneOffset.UTC).minusDays(7))).isEqualTo(1);
        assertThat(count("topic = 't.old'")).isZero();
        assertThat(count("topic = 't.new'")).isEqualTo(1);
        assertThat(count("topic = 't.unsent'")).as("an unsent event is never deleted, however old").isEqualTo(1);
    }
}
