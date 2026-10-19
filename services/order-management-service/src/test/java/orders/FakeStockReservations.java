package orders;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

class FakeStockReservations implements StockReservations {

    StockReservationResult result = StockReservationResult.RESERVED;
    RuntimeException failure;
    String lastAuthorization;
    List<LineItem> lastItems = List.of();
    final List<UUID> reservedOrders = new CopyOnWriteArrayList<>();

    void reset() {
        result = StockReservationResult.RESERVED;
        failure = null;
        lastAuthorization = null;
        lastItems = List.of();
        reservedOrders.clear();
    }

    @Override
    public StockReservationResult reserve(UUID orderId, List<LineItem> items, String authorization) {
        if (failure != null) {
            throw failure;
        }
        lastAuthorization = authorization;
        lastItems = items;
        reservedOrders.add(orderId);
        return result;
    }
}
