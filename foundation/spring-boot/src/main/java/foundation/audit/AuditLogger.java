package foundation.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Writes one line per audit event to the logger {@code audit}: {@code audit actor=.. action=.. resource=.. outcome=..}. Values are
 * stripped of line breaks and other control characters and cut at 200 characters, so a caller-supplied value cannot forge a second
 * line; the line goes through the same masking as every log message.
 */
@Component
public class AuditLogger {

    static final String LOGGER = "audit";
    private static final int MAX = 200;
    private final Logger log = LoggerFactory.getLogger(LOGGER);

    public void record(AuditEvent event) {
        log.info("audit actor={} action={} resource={} outcome={}", clean(event.actor()), clean(event.action()), clean(event.resource()), clean(event.outcome()));
    }

    /** Records an event for the signed-in user ({@code anonymous} when there is none). */
    public void record(String action, String resource, String outcome) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication != null && authentication.isAuthenticated() ? authentication.getName() : "anonymous";
        record(new AuditEvent(actor, action, resource, outcome));
    }

    static String clean(String value) {
        if (value == null) {
            return "-";
        }
        StringBuilder out = new StringBuilder();
        value.codePoints().limit(MAX).forEach(cp -> out.appendCodePoint(Character.isISOControl(cp) ? '_' : cp));
        return out.isEmpty() ? "-" : out.toString();
    }
}
