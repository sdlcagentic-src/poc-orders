package foundation.messaging;

/** An event was not accepted by the broker. */
public class EventSinkException extends RuntimeException {

    public EventSinkException(String message, Throwable cause) {
        super(message, cause);
    }
}
