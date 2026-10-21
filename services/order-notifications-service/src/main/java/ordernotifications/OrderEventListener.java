package ordernotifications;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private final NotificationService notifications;

    public OrderEventListener(NotificationService notifications) {
        this.notifications = notifications;
    }

    @KafkaListener(topics = "orders.confirmed", groupId = "order-notifications-service")
    public void onOrderConfirmed(ConsumerRecord<String, String> record) {
        notifications.orderConfirmed(record.value());
    }

    @KafkaListener(topics = "orders.rejected", groupId = "order-notifications-service")
    public void onOrderRejected(ConsumerRecord<String, String> record) {
        notifications.orderRejected(record.value());
    }

    @KafkaListener(topics = "orders.cancelled", groupId = "order-notifications-service")
    public void onOrderCancelled(ConsumerRecord<String, String> record) {
        notifications.orderCancelled(record.value());
    }
}
