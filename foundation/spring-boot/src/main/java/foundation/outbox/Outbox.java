package foundation.outbox;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The transactional outbox. {@link #publish} writes the event to the table {@code outbox_event} in the caller's database transaction, so the
 * event exists exactly when the caller's state change does; {@link OutboxRelay} then sends it to the broker (at least once).
 */
public class Outbox {

    private static final Pattern TOPIC = Pattern.compile("[A-Za-z0-9._-]{1,249}");
    private static final int MAX_KEY = 255;

    private final JdbcClient jdbc;

    public Outbox(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Records an event for {@code topic}; {@code key} (may be null) decides the partition. Must run inside a transaction. */
    public void publish(String topic, String key, String payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Outbox.publish must run inside the transaction of the state change it belongs to");
        }
        if (topic == null || !TOPIC.matcher(topic).matches()) {
            throw new IllegalArgumentException("not a topic name: " + topic);
        }
        if (payload == null) {
            throw new IllegalArgumentException("an event needs a payload");
        }
        if (key != null && key.length() > MAX_KEY) {
            throw new IllegalArgumentException("an event key may have at most " + MAX_KEY + " characters");
        }
        jdbc.sql("INSERT INTO outbox_event (id, topic, message_key, payload, created_at) VALUES (:id, :topic, :key, :payload, :createdAt)")
                .param("id", UUID.randomUUID().toString())
                .param("topic", topic)
                .param("key", key, Types.VARCHAR)
                .param("payload", payload)
                .param("createdAt", OffsetDateTime.now(ZoneOffset.UTC))
                .update();
    }
}
