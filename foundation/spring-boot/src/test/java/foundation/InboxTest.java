package foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import foundation.outbox.Inbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class InboxTest {

    @Autowired
    Inbox inbox;
    @Autowired
    JdbcClient jdbc;
    @Autowired
    PlatformTransactionManager transactionManager;

    @BeforeEach
    void clean() {
        jdbc.sql("DELETE FROM processed_message").update();
    }

    @Test
    void aMessageIsNewOnlyTheFirstTimeForAConsumer() {
        assertThat(inbox.firstTime("consumer-a", "m-1")).isTrue();
        assertThat(inbox.firstTime("consumer-a", "m-1")).isFalse();
        assertThat(inbox.firstTime("consumer-a", "m-2")).isTrue();
        assertThat(inbox.firstTime("consumer-b", "m-1")).as("another consumer has its own record").isTrue();
    }

    @Test
    void theRecordCommitsOrRollsBackWithTheHandlersTransaction() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            assertThat(inbox.firstTime("consumer-a", "m-9")).isTrue();
            throw new IllegalStateException("the handler failed");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(inbox.firstTime("consumer-a", "m-9")).as("the failed attempt left no record, so the retry is first").isTrue();
    }

    @Test
    void badInputIsRefused() {
        assertThatThrownBy(() -> inbox.firstTime(null, "m")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inbox.firstTime("c", " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inbox.firstTime("c".repeat(101), "m")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inbox.firstTime("c", "m".repeat(256))).isInstanceOf(IllegalArgumentException.class);
    }
}
