package foundation.messaging;

/**
 * Where events go. The foundation ships a Kafka implementation; another broker is another implementation. {@code send} returns only when
 * the broker has accepted the event, and throws {@link EventSinkException} when it has not.
 */
public interface EventSink {

    void send(String topic, String key, String payload);
}
