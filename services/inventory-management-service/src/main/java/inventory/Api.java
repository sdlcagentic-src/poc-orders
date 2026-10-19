package inventory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class Api {

    private Api() {
    }

    public record ReservationItem(UUID productId, Integer quantity) {
    }

    public record ReserveStockRequest(UUID orderId, List<ReservationItem> items) {
    }

    public record Reservation(UUID reservationId, UUID orderId, List<ReservationItem> items,
            OffsetDateTime reservedAt) {
    }

    public record StockLevel(UUID productId, int quantity) {
    }

    public record StockLevelList(List<StockLevel> items) {
    }
}
