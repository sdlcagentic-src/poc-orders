package orders;

import java.util.List;
import java.util.UUID;

/**
 * What the order service needs from inventory. Failures to reach or be authorised by
 * inventory surface as exceptions so that no order is stored without a decision.
 */
public interface StockReservations {

    StockReservationResult reserve(UUID orderId, List<LineItem> items, String authorization);
}
