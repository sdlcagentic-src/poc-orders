package orders;

import java.util.List;

public record CreateOrderRequest(String customerEmail, List<LineItem> lineItems) {
}
