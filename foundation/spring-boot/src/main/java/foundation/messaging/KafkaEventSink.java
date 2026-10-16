package foundation.messaging;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.kafka.core.KafkaTemplate;

/** Sends to Kafka and waits for the broker's acknowledgement, so a caller that gets no exception knows the event is stored. */
public class KafkaEventSink implements EventSink {

    private final KafkaTemplate<String, String> template;
    private final Duration timeout;

    public KafkaEventSink(KafkaTemplate<String, String> template, Duration timeout) {
        this.template = template;
        this.timeout = timeout;
    }

    @Override
    public void send(String topic, String key, String payload) {
        try {
            template.send(topic, key, payload).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventSinkException("interrupted while sending to " + topic, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new EventSinkException("could not send to " + topic, e);
        } catch (RuntimeException e) {
            // the producer can also fail before it returns a future (no metadata in time, a closed producer, a bad record)
            throw new EventSinkException("could not send to " + topic, e);
        }
    }
}
