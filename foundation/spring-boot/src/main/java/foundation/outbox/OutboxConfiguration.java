package foundation.outbox;

import foundation.messaging.EventSink;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** The outbox, its relay and its cleaner. The tables come from the migrations in {@code db/foundation} (see the README). */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class OutboxConfiguration {

    @Bean
    Outbox outbox(JdbcClient jdbc) {
        return new Outbox(jdbc);
    }

    @Bean
    OutboxRelay outboxRelay(JdbcClient jdbc, EventSink sink, PlatformTransactionManager transactionManager,
                            @Value("${foundation.outbox.batch-size:100}") int batchSize) {
        return new OutboxRelay(jdbc, sink, new TransactionTemplate(transactionManager), batchSize);
    }

    @Bean
    OutboxCleaner outboxCleaner(JdbcClient jdbc, @Value("${foundation.outbox.retention-days:7}") int retentionDays) {
        return new OutboxCleaner(jdbc, retentionDays);
    }

    @Bean
    Inbox inbox(JdbcClient jdbc) {
        return new Inbox(jdbc);
    }
}
