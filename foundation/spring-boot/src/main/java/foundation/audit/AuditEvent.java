package foundation.audit;

/** Who did what to which resource, and how it ended. The values are free text from the caller and are cleaned before they are logged. */
public record AuditEvent(String actor, String action, String resource, String outcome) {
}
