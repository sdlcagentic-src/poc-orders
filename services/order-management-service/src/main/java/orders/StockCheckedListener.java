package orders;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class StockCheckedListener {

    private final OrderService service;

    StockCheckedListener(OrderService service) {
        this.service = service;
    }

    @KafkaListener(topics = "inventory.stock.checked", groupId = "order-management-service")
    void onStockChecked(ConsumerRecord<String, String> record) {
        String messageId = record.topic() + "-" + record.partition() + "-" + record.offset();
        service.applyStockChecked(messageId, record.value());
    }
}
