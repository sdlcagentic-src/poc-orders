package orders;

import java.util.List;

public record OrderView(String orderId, String status, List<LineItem> lineItems, String placedAt) {
}
