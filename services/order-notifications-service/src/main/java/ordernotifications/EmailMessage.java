package ordernotifications;

/**
 * An email to dispatch. The address is kept only for the duration of the send;
 * toString deliberately omits it so that it cannot reach a log line.
 */
public record EmailMessage(String to, String subject, String body) {

    @Override
    public String toString() {
        return "EmailMessage[subject=" + subject + "]";
    }
}
