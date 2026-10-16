package foundation.outbox;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;

/** Deletes events that were sent more than the retention period ago ({@code foundation.outbox.retention-days}, 7 by default). Unsent events are never deleted. */
public class OutboxCleaner {

    private final JdbcClient jdbc;
    private final int retentionDays;

    public OutboxCleaner(JdbcClient jdbc, int retentionDays) {
        this.jdbc = jdbc;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${foundation.outbox.cleanup-cron:0 0 3 * * *}")
    public void clean() {
        cleanOlderThan(OffsetDateTime.now(ZoneOffset.UTC).minusDays(retentionDays));
    }

    /** Deletes the events sent before {@code cutoff}; returns how many. */
    public int cleanOlderThan(OffsetDateTime cutoff) {
        return jdbc.sql("DELETE FROM outbox_event WHERE published_at IS NOT NULL AND published_at < :cutoff").param("cutoff", cutoff).update();
    }
}
