package ordernotifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
class NotificationServiceTest {

    private static final String EMAIL = "customer@example.com";

    @TestConfiguration
    static class Doubles {
        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
        }
    }

    @Autowired NotificationService service;
    @Autowired OrderEventListener listener;
    @Autowired RecordingEmailSender sender;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void setUp() {
        sender.reset();
    }

    private List<String> outboxRowsFor(UUID orderId) {
        return jdbc.sql("select * from outbox_event").query().listOfRows().stream()
                .map(Object::toString)
                .filter(row -> row.contains(orderId.toString()))
                .toList();
    }

    private static String confirmed(UUID id) {
        return "{\"orderId\":\"" + id + "\",\"customerEmail\":\"" + EMAIL
                + "\",\"confirmedAt\":\"2025-01-01T10:00:00Z\"}";
    }

    private static String rejected(UUID id, UUID product) {
        return "{\"orderId\":\"" + id + "\",\"customerEmail\":\"" + EMAIL
                + "\",\"unavailableProductIds\":[\"" + product + "\"],\"rejectedAt\":\"2025-01-01T10:00:00Z\"}";
    }

    private static String cancelled(UUID id) {
        return "{\"orderId\":\"" + id + "\",\"customerEmail\":\"" + EMAIL
                + "\",\"cancelledAt\":\"2025-01-01T10:00:00Z\"}";
    }

    @Test
    void confirmedOrderSendsConfirmationEmailWithOrderDetails() {
        UUID id = UUID.randomUUID();
        service.orderConfirmed(confirmed(id));

        assertEquals(1, sender.sent.size());
        EmailMessage message = sender.sent.get(0);
        assertEquals(EMAIL, message.to());
        assertTrue(message.body().contains(id.toString()));
        assertTrue(message.subject().contains("confirmed"));
    }

    @Test
    void confirmedOrderAnnouncesConfirmationEmailSent() {
        UUID id = UUID.randomUUID();
        service.orderConfirmed(confirmed(id));

        List<String> rows = outboxRowsFor(id);
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).contains("notifications.confirmation.sent"));
    }

    @Test
    void rejectedOrderSendsEmailExplainingOutOfStockItems() {
        UUID id = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        service.orderRejected(rejected(id, product));

        assertEquals(1, sender.sent.size());
        EmailMessage message = sender.sent.get(0);
        assertEquals(EMAIL, message.to());
        assertTrue(message.body().contains("out of stock"));
        assertTrue(message.body().contains(product.toString()));
    }

    @Test
    void rejectedOrderAnnouncesRejectionEmailSent() {
        UUID id = UUID.randomUUID();
        service.orderRejected(rejected(id, UUID.randomUUID()));

        List<String> rows = outboxRowsFor(id);
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).contains("notifications.rejection.sent"));
    }

    @Test
    void cancelledOrderSendsCancellationEmail() {
        UUID id = UUID.randomUUID();
        service.orderCancelled(cancelled(id));

        assertEquals(1, sender.sent.size());
        assertEquals(EMAIL, sender.sent.get(0).to());
        assertTrue(sender.sent.get(0).body().contains("cancellation"));
    }

    @Test
    void cancelledOrderAnnouncesCancellationEmailSent() {
        UUID id = UUID.randomUUID();
        service.orderCancelled(cancelled(id));

        List<String> rows = outboxRowsFor(id);
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).contains("notifications.cancellation.sent"));
    }

    @Test
    void emailAddressIsNotStoredInTheOutbox() {
        UUID id = UUID.randomUUID();
        service.orderConfirmed(confirmed(id));

        assertFalse(outboxRowsFor(id).isEmpty());
        assertTrue(outboxRowsFor(id).stream().noneMatch(row -> row.contains(EMAIL)));
    }

    @Test
    void emailMessageTextDoesNotExposeTheAddress() {
        EmailMessage message = new EmailMessage(EMAIL, "subject", "body");
        assertFalse(message.toString().contains(EMAIL));
    }

    @Test
    void redeliveredEventSendsOnlyOneEmail() {
        UUID id = UUID.randomUUID();
        service.orderConfirmed(confirmed(id));
        service.orderConfirmed(confirmed(id));

        assertEquals(1, sender.sent.size());
        assertEquals(1, outboxRowsFor(id).size());
    }

    @Test
    void failedSendLeavesNothingRecordedSoTheEventCanBeRetried() {
        UUID id = UUID.randomUUID();
        sender.failing = true;
        assertThrows(IllegalStateException.class, () -> service.orderConfirmed(confirmed(id)));
        assertTrue(outboxRowsFor(id).isEmpty());

        sender.failing = false;
        service.orderConfirmed(confirmed(id));
        assertEquals(1, sender.sent.size());
    }

    @Test
    void payloadWithoutOrderIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.orderConfirmed("{\"customerEmail\":\"" + EMAIL + "\",\"confirmedAt\":\"x\"}"));
        assertTrue(sender.sent.isEmpty());
    }

    @Test
    void confirmedListenerHandlesRecord() {
        UUID id = UUID.randomUUID();
        listener.onOrderConfirmed(new ConsumerRecord<>("orders.confirmed", 0, 0L, id.toString(), confirmed(id)));
        assertEquals(1, sender.sent.size());
    }

    @Test
    void rejectedListenerHandlesRecord() {
        UUID id = UUID.randomUUID();
        listener.onOrderRejected(new ConsumerRecord<>("orders.rejected", 0, 0L, id.toString(),
                rejected(id, UUID.randomUUID())));
        assertEquals(1, sender.sent.size());
    }

    @Test
    void cancelledListenerHandlesRecord() {
        UUID id = UUID.randomUUID();
        listener.onOrderCancelled(new ConsumerRecord<>("orders.cancelled", 0, 0L, id.toString(), cancelled(id)));
        assertEquals(1, sender.sent.size());
    }
}
