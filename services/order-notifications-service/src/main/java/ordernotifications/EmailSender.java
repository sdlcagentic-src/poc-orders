package ordernotifications;

/** Port for dispatching an email to a customer. */
public interface EmailSender {

    /**
     * Sends the message; any failure must surface as an exception so that the
     * triggering event is retried rather than silently dropped.
     */
    void send(EmailMessage message);
}
