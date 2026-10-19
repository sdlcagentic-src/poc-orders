package orders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import foundation.outbox.Inbox;
import foundation.outbox.Outbox;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private static final String PLACED = "placed";
    private static final String CONFIRMED = "confirmed";
    private static final String REJECTED = "rejected";
    private static final String CANCELLED = "cancelled";
    private static final String STOCK_CHECKED_CONSUMER = "order-management-service.stock-checked";

    private final OrderRepository repository;
    private final StockReservations reservations;
    private final Outbox outbox;
    private final Inbox inbox;
    private final TransactionTemplate tx;

    OrderService(OrderRepository repository, StockReservations reservations, Outbox outbox, Inbox inbox,
                 PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.reservations = reservations;
        this.outbox = outbox;
        this.inbox = inbox;
        this.tx = new TransactionTemplate(transactionManager);
    }

    OrderView create(CreateOrderRequest request, String authorization) {
        validate(request);
        UUID orderId = UUID.randomUUID();
        List<LineItem> items = List.copyOf(request.lineItems());

        // The reservation is decided before anything is stored, so a failed call leaves no order behind.
        boolean reserved = reservations.reserve(orderId, items, authorization) == StockReservationResult.RESERVED;

        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        StoredOrder order = new StoredOrder(orderId.toString(), request.customerEmail().trim(),
                reserved ? CONFIRMED : REJECTED, now, items);
        tx.executeWithoutResult(status -> {
            repository.insert(order);
            outbox.publish("orders.placed", order.orderId(), placedPayload(order));
            if (reserved) {
                outbox.publish("orders.confirmed", order.orderId(), confirmedPayload(order, now));
            } else {
                outbox.publish("orders.rejected", order.orderId(), rejectedPayload(order, List.of(), now));
            }
        });
        return view(order);
    }

    OrderView get(String orderId) {
        return view(repository.find(requireKnownId(orderId)).orElseThrow(OrderService::notFound));
    }

    OrderView cancel(String orderId) {
        String id = requireKnownId(orderId);
        return tx.execute(status -> {
            StoredOrder order = repository.find(id).orElseThrow(OrderService::notFound);
            boolean cancellable = PLACED.equals(order.status()) || CONFIRMED.equals(order.status());
            if (!cancellable || !repository.transition(id, order.status(), CANCELLED)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The order can no longer be cancelled.");
            }
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            outbox.publish("orders.cancelled", id, "{\"orderId\":" + Json.quote(id)
                    + ",\"customerEmail\":" + Json.quote(order.customerEmail())
                    + ",\"cancelledAt\":" + Json.quote(now.toString()) + "}");
            return view(new StoredOrder(id, order.customerEmail(), CANCELLED, order.placedAt(), order.items()));
        });
    }

    void applyStockChecked(String messageId, String payload) {
        tx.executeWithoutResult(status -> {
            if (!inbox.firstTime(STOCK_CHECKED_CONSUMER, messageId)) {
                return;
            }
            Json.StockChecked result = Json.parseStockChecked(payload);
            StoredOrder order = repository.find(result.orderId()).orElse(null);
            if (order == null || !PLACED.equals(order.status())) {
                return;
            }
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            if (result.allAvailable()) {
                if (repository.transition(order.orderId(), PLACED, CONFIRMED)) {
                    outbox.publish("orders.confirmed", order.orderId(), confirmedPayload(order, now));
                }
            } else if (repository.transition(order.orderId(), PLACED, REJECTED)) {
                outbox.publish("orders.rejected", order.orderId(),
                        rejectedPayload(order, result.unavailableProductIds(), now));
            }
        });
    }

    private static void validate(CreateOrderRequest request) {
        if (request == null || request.customerEmail() == null || request.customerEmail().isBlank()
                || !request.customerEmail().contains("@") || request.lineItems() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The request is malformed.");
        }
        if (request.lineItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "An order needs at least one line item.");
        }
        for (LineItem item : request.lineItems()) {
            if (item == null || item.productId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The request is malformed.");
            }
            if (item.quantity() == null || item.quantity() < 1) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "A quantity must be at least 1.");
            }
        }
    }

    private static String requireKnownId(String orderId) {
        try {
            return UUID.fromString(orderId).toString();
        } catch (IllegalArgumentException e) {
            throw notFound();
        }
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "The order does not exist.");
    }

    private static OrderView view(StoredOrder order) {
        return new OrderView(order.orderId(), order.status(), order.items(), order.placedAt().toString());
    }

    private static String placedPayload(StoredOrder order) {
        return "{\"orderId\":" + Json.quote(order.orderId())
                + ",\"lineItems\":" + Json.itemsArray(order.items())
                + ",\"placedAt\":" + Json.quote(order.placedAt().toString()) + "}";
    }

    private static String confirmedPayload(StoredOrder order, Instant at) {
        return "{\"orderId\":" + Json.quote(order.orderId())
                + ",\"customerEmail\":" + Json.quote(order.customerEmail())
                + ",\"confirmedAt\":" + Json.quote(at.toString()) + "}";
    }

    private static String rejectedPayload(StoredOrder order, List<String> unavailable, Instant at) {
        return "{\"orderId\":" + Json.quote(order.orderId())
                + ",\"customerEmail\":" + Json.quote(order.customerEmail())
                + ",\"unavailableProductIds\":" + Json.idArray(unavailable)
                + ",\"rejectedAt\":" + Json.quote(at.toString()) + "}";
    }
}
