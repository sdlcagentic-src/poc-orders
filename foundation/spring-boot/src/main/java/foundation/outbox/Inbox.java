package foundation.outbox;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Lets a consumer skip a message it has already handled. Call {@link #firstTime} inside the same transaction as the handler's database
 * work: it records the message and answers true only the first time, so the record and the work commit or roll back together.
 * Uses {@code INSERT .. ON CONFLICT DO NOTHING}, which PostgreSQL supports (and so does H2 in its PostgreSQL mode).
 */
public class Inbox {

    private static final int MAX = 255;

    private final JdbcClient jdbc;

    public Inbox(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** True the first time this consumer sees {@code messageId}, false on every later call. */
    public boolean firstTime(String consumer, String messageId) {
        if (consumer == null || consumer.isBlank() || consumer.length() > 100 || messageId == null || messageId.isBlank() || messageId.length() > MAX) {
            throw new IllegalArgumentException("a consumer name (up to 100 characters) and a message id (up to " + MAX + ") are needed");
        }
        return jdbc.sql("INSERT INTO processed_message (consumer, message_id, processed_at) VALUES (:consumer, :messageId, :now) "
                        + "ON CONFLICT DO NOTHING")
                .param("consumer", consumer)
                .param("messageId", messageId)
                .param("now", OffsetDateTime.now(ZoneOffset.UTC))
                .update() == 1;
    }
}
