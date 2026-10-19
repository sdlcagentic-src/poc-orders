package inventory;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import foundation.outbox.Outbox;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class InventoryService {

    record Outcome(Api.Reservation reservation, List<UUID> unavailable) {
    }

    private record Header(UUID reservationId, OffsetDateTime reservedAt) {
    }

    private final JdbcClient jdbc;
    private final Outbox outbox;
    private final Clock clock;

    InventoryService(JdbcClient jdbc, Outbox outbox, Clock clock) {
        this.jdbc = jdbc;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    Outcome reserve(Api.ReserveStockRequest request) {
        Map<UUID, Integer> wanted = validate(request);
        UUID orderId = request.orderId();

        Optional<Api.Reservation> existing = findReservation(orderId);
        if (existing.isPresent()) {
            return new Outcome(existing.get(), List.of());
        }

        for (UUID productId : wanted.keySet()) {
            Integer found = jdbc.sql("select count(*) from stock_level where product_id = :p")
                    .param("p", productId).query(Integer.class).single();
            if (found == 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Unknown product: " + productId);
            }
        }

        // The guarded update keeps stock non-negative; the row lock serialises concurrent orders.
        List<UUID> applied = new ArrayList<>();
        List<UUID> unavailable = new ArrayList<>();
        wanted.forEach((productId, quantity) -> {
            int rows = jdbc.sql("update stock_level set quantity = quantity - :q where product_id = :p and quantity >= :q")
                    .param("q", quantity).param("p", productId).update();
            if (rows == 1) {
                applied.add(productId);
            } else {
                unavailable.add(productId);
            }
        });

        if (!unavailable.isEmpty()) {
            for (UUID productId : applied) {
                jdbc.sql("update stock_level set quantity = quantity + :q where product_id = :p")
                        .param("q", wanted.get(productId)).param("p", productId).update();
            }
            outbox.publish("inventory.stock.checked", orderId.toString(), checkedPayload(orderId, false, unavailable));
            return new Outcome(null, unavailable);
        }

        UUID reservationId = UUID.randomUUID();
        OffsetDateTime reservedAt = OffsetDateTime.now(clock);
        jdbc.sql("insert into reservation (reservation_id, order_id, reserved_at) values (:r, :o, :t)")
                .param("r", reservationId).param("o", orderId).param("t", reservedAt).update();
        List<Api.ReservationItem> items = new ArrayList<>();
        wanted.forEach((productId, quantity) -> {
            jdbc.sql("insert into reservation_item (reservation_id, product_id, quantity) values (:r, :p, :q)")
                    .param("r", reservationId).param("p", productId).param("q", quantity).update();
            items.add(new Api.ReservationItem(productId, quantity));
        });
        outbox.publish("inventory.stock.checked", orderId.toString(), checkedPayload(orderId, true, List.of()));
        return new Outcome(new Api.Reservation(reservationId, orderId, items, reservedAt), List.of());
    }

    @Transactional
    boolean release(UUID orderId) {
        Optional<Api.Reservation> reservation = findReservation(orderId);
        if (reservation.isEmpty()) {
            return false;
        }
        String releasedAt = Instant.now(clock).toString();
        for (Api.ReservationItem item : reservation.get().items()) {
            jdbc.sql("update stock_level set quantity = quantity + :q where product_id = :p")
                    .param("q", item.quantity()).param("p", item.productId()).update();
            String payload = "{\"orderId\":\"" + orderId + "\",\"productId\":\"" + item.productId()
                    + "\",\"quantity\":" + item.quantity() + ",\"releasedAt\":\"" + releasedAt + "\"}";
            outbox.publish("inventory.stock.released", orderId.toString(), payload);
        }
        jdbc.sql("delete from reservation_item where reservation_id = :r")
                .param("r", reservation.get().reservationId()).update();
        jdbc.sql("delete from reservation where reservation_id = :r")
                .param("r", reservation.get().reservationId()).update();
        return true;
    }

    List<Api.StockLevel> stockLevels() {
        return jdbc.sql("select product_id, quantity from stock_level order by product_id")
                .query((rs, n) -> new Api.StockLevel(rs.getObject("product_id", UUID.class), rs.getInt("quantity")))
                .list();
    }

    private Optional<Api.Reservation> findReservation(UUID orderId) {
        Optional<Header> header = jdbc.sql("select reservation_id, reserved_at from reservation where order_id = :o")
                .param("o", orderId)
                .query((rs, n) -> new Header(rs.getObject("reservation_id", UUID.class),
                        rs.getObject("reserved_at", OffsetDateTime.class)))
                .optional();
        return header.map(h -> {
            List<Api.ReservationItem> items = jdbc
                    .sql("select product_id, quantity from reservation_item where reservation_id = :r order by product_id")
                    .param("r", h.reservationId())
                    .query((rs, n) -> new Api.ReservationItem(rs.getObject("product_id", UUID.class), rs.getInt("quantity")))
                    .list();
            return new Api.Reservation(h.reservationId(), orderId, items, h.reservedAt());
        });
    }

    private Map<UUID, Integer> validate(Api.ReserveStockRequest request) {
        if (request == null || request.orderId() == null || request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId and at least one item are required");
        }
        Map<UUID, Integer> wanted = new TreeMap<>();
        for (Api.ReservationItem item : request.items()) {
            if (item == null || item.productId() == null || item.quantity() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each item needs a productId and a quantity");
            }
            if (item.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Quantity must be positive");
            }
            wanted.merge(item.productId(), item.quantity(), Integer::sum);
        }
        return wanted;
    }

    private String checkedPayload(UUID orderId, boolean allAvailable, List<UUID> unavailable) {
        StringBuilder ids = new StringBuilder();
        for (UUID id : unavailable) {
            if (ids.length() > 0) {
                ids.append(',');
            }
            ids.append('"').append(id).append('"');
        }
        return "{\"orderId\":\"" + orderId + "\",\"allAvailable\":" + allAvailable
                + ",\"unavailableProductIds\":[" + ids + "]}";
    }
}
