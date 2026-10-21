package ordernotifications;

import foundation.outbox.Inbox;
import foundation.outbox.Outbox;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns order outcome events into customer emails and announces each dispatch.
 * The customer address is used only to send and is never stored or logged.
 */
@Service
public class NotificationService {

    static final String CONFIRMATION_SENT = "notifications.confirmation.sent";
    static final String REJECTION_SENT = "notifications.rejection.sent";
    static final String CANCELLATION_SENT = "notifications.cancellation.sent";

    private final EmailSender emailSender;
    private final Outbox outbox;
    private final Inbox inbox;

    public NotificationService(EmailSender emailSender, Outbox outbox, Inbox inbox) {
        this.emailSender = emailSender;
        this.outbox = outbox;
        this.inbox = inbox;
    }

    @Transactional
    public void orderConfirmed(String payload) {
        Map<String, Object> event = parse(payload);
        UUID orderId = orderId(event);
        String to = required(event, "customerEmail");
        String confirmedAt = required(event, "confirmedAt");
        if (!inbox.firstTime("order-confirmed-email", orderId.toString())) {
            return;
        }
        String body = "Your order " + orderId + " was confirmed on " + confirmedAt
                + ". All items are in stock and reserved for you.";
        dispatch(new EmailMessage(to, "Your order " + orderId + " is confirmed", body), CONFIRMATION_SENT, orderId);
    }

    @Transactional
    public void orderRejected(String payload) {
        Map<String, Object> event = parse(payload);
        UUID orderId = orderId(event);
        String to = required(event, "customerEmail");
        String rejectedAt = required(event, "rejectedAt");
        String products = unavailable(event);
        if (!inbox.firstTime("order-rejected-email", orderId.toString())) {
            return;
        }
        String body = "Your order " + orderId + " could not be accepted on " + rejectedAt
                + " because these items are out of stock: " + products + ".";
        dispatch(new EmailMessage(to, "Your order " + orderId + " was not accepted", body), REJECTION_SENT, orderId);
    }

    @Transactional
    public void orderCancelled(String payload) {
        Map<String, Object> event = parse(payload);
        UUID orderId = orderId(event);
        String to = required(event, "customerEmail");
        String cancelledAt = required(event, "cancelledAt");
        if (!inbox.firstTime("order-cancelled-email", orderId.toString())) {
            return;
        }
        String body = "Your cancellation of order " + orderId + " was processed on " + cancelledAt + ".";
        dispatch(new EmailMessage(to, "Your order " + orderId + " is cancelled", body), CANCELLATION_SENT, orderId);
    }

    private void dispatch(EmailMessage message, String topic, UUID orderId) {
        emailSender.send(message);
        String sent = "{\"orderId\":\"" + orderId + "\",\"sentAt\":\"" + Instant.now() + "\"}";
        outbox.publish(topic, orderId.toString(), sent);
    }

    private static Map<String, Object> parse(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Empty event payload");
        }
        return JsonParserFactory.getJsonParser().parseMap(payload);
    }

    private static UUID orderId(Map<String, Object> event) {
        return UUID.fromString(required(event, "orderId"));
    }

    private static String required(Map<String, Object> event, String field) {
        Object value = event.get(field);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Event field missing: " + field);
        }
        return value.toString();
    }

    private static String unavailable(Map<String, Object> event) {
        Object value = event.get("unavailableProductIds");
        if (!(value instanceof List<?> ids)) {
            throw new IllegalArgumentException("Event field missing: unavailableProductIds");
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }
}
