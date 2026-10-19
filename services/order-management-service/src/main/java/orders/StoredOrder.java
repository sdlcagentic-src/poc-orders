package orders;

import java.time.Instant;
import java.util.List;

record StoredOrder(String orderId, String customerEmail, String status, Instant placedAt, List<LineItem> items) {
}
