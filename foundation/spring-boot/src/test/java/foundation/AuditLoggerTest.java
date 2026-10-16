package foundation;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import foundation.audit.AuditEvent;
import foundation.audit.AuditLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AuditLoggerTest {

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final Logger audit = (Logger) LoggerFactory.getLogger("audit");

    @BeforeEach
    void attach() {
        appender.start();
        audit.addAppender(appender);
    }

    @AfterEach
    void detach() {
        audit.detachAppender(appender);
    }

    @Test
    void anEventIsOneLineWithActorActionResourceAndOutcome() {
        new AuditLogger().record(new AuditEvent("alice", "cancel-item", "item/42", "success"));
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).isEqualTo("audit actor=alice action=cancel-item resource=item/42 outcome=success");
    }

    @Test
    void aValueCannotForgeASecondLine() {
        new AuditLogger().record(new AuditEvent("mallory\naudit actor=admin action=grant", "x", "y", "z"));
        assertThat(appender.list.get(0).getFormattedMessage()).doesNotContain("\n").contains("mallory_audit actor=admin action=grant");
    }

    @Test
    void missingValuesAndVeryLongValuesAreHandled() {
        new AuditLogger().record(new AuditEvent(null, "a".repeat(500), "", "ok"));
        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message).startsWith("audit actor=- action=" + "a".repeat(200) + " resource=- outcome=ok");
    }

    @Test
    void withoutASignedInUserTheActorIsAnonymous() {
        new AuditLogger().record("login", "session", "failure");
        assertThat(appender.list.get(0).getFormattedMessage()).startsWith("audit actor=anonymous action=login");
    }
}
