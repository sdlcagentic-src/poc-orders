package foundation;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import foundation.logging.MaskingJsonEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MaskingJsonEncoderTest {

    private final Logger logger = context().getLogger("items");

    private static LoggerContext context() {
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new org.slf4j.helpers.BasicMDCAdapter());
        return context;
    }

    private String encode(String mode, String message, Throwable error) {
        MaskingJsonEncoder encoder = new MaskingJsonEncoder();
        encoder.setMasking(mode);
        LoggingEvent event = new LoggingEvent(Logger.class.getName(), logger, Level.INFO, message, error, null);
        return new String(encoder.encode(event), StandardCharsets.UTF_8);
    }

    @Test
    void oneLineOfJsonWithTheUsualFields() {
        String line = encode("email", "item created", null);
        assertThat(line).endsWith("}\n").startsWith("{\"timestamp\":\"").contains("\"level\":\"INFO\"").contains("\"logger\":\"items\"")
                .contains("\"message\":\"item created\"");
        assertThat(line.strip()).doesNotContain("\n");
    }

    @Test
    void emailAddressesAreRedactedInMessagesAndStackTraces() {
        String line = encode("email", "sent to jane.doe@example.com now", new IllegalStateException("bad address bob@example.org"));
        assertThat(line).contains("sent to [email] now").doesNotContain("jane.doe@example.com").doesNotContain("bob@example.org");
    }

    @Test
    void phoneNumbersAreRedactedOnlyWhenAsked() {
        assertThat(encode("email-phone", "call +46 70 123 45 67 please", null)).contains("call [phone] please");
        assertThat(encode("email", "call +46 70 123 45 67 please", null)).contains("+46 70 123 45 67");
    }

    @Test
    void noneKeepsTheMessageAsItIs() {
        assertThat(encode("none", "mail to jane@example.com", null)).contains("mail to jane@example.com");
    }

    @Test
    void lineBreaksAndQuotesInAMessageCannotBreakOutOfTheJsonString() {
        String line = encode("none", "a\"b\nc\\d", null);
        assertThat(line).contains("\"message\":\"a\\\"b\\nc\\\\d\"");
        assertThat(line.strip()).doesNotContain("\n");
    }
}
