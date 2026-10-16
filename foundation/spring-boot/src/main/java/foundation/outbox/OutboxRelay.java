package foundation.outbox;

import foundation.messaging.EventSink;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Sends the events that are not yet sent, oldest first, and marks each as sent after the broker accepted it. Delivery is at least once: if
 * the service stops between the send and the mark, the event is sent again, so consumers must tolerate a repeat. Rows are locked with
 * {@code FOR UPDATE SKIP LOCKED}, so several instances can relay without sending the same row twice at the same moment (the sequence of
 * events with the same key is only guaranteed while one instance relays). A failed send stops the batch and is tried again next time.
 */
public class OutboxRelay {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);

    private record Row(String id, String topic, String key, String payload) {
    }

    private final JdbcClient jdbc;
    private final EventSink sink;
    private final TransactionTemplate transaction;
    private final int batchSize;

    public OutboxRelay(JdbcClient jdbc, EventSink sink, TransactionTemplate transaction, int batchSize) {
        this.jdbc = jdbc;
        this.sink = sink;
        this.transaction = transaction;
        this.batchSize = batchSize;
    }

    @Scheduled(initialDelayString = "${foundation.outbox.initial-delay-ms:0}", fixedDelayString = "${foundation.outbox.poll-interval-ms:500}")
    public void poll() {
        relayOnce();
    }

    /** Sends one batch; returns how many events were sent and marked. */
    public int relayOnce() {
        Integer sent = transaction.execute(status -> sendBatch());
        return sent == null ? 0 : sent;
    }

    private int sendBatch() {
        List<Row> rows = jdbc.sql("SELECT id, topic, message_key, payload FROM outbox_event WHERE published_at IS NULL "
                        + "ORDER BY created_at, id LIMIT :limit FOR UPDATE SKIP LOCKED")
                .param("limit", batchSize)
                .query((rs, n) -> new Row(rs.getString("id"), rs.getString("topic"), rs.getString("message_key"), rs.getString("payload")))
                .list();
        int sent = 0;
        for (Row row : rows) {
            try {
                sink.send(row.topic(), row.key(), row.payload());
            } catch (RuntimeException e) {
                LOG.warn("outbox event {} was not sent to {} and will be tried again: {}", row.id(), row.topic(), e.getMessage());
                break;
            }
            jdbc.sql("UPDATE outbox_event SET published_at = :now WHERE id = :id")
                    .param("now", OffsetDateTime.now(ZoneOffset.UTC))
                    .param("id", row.id())
                    .update();
            sent++;
        }
        return sent;
    }
}
