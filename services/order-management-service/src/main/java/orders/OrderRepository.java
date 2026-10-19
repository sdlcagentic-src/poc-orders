package orders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class OrderRepository {

    private record Header(String email, String status, String placedAt) {
    }

    private final JdbcClient jdbc;

    OrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    void insert(StoredOrder order) {
        jdbc.sql("insert into orders (order_id, customer_email, status, placed_at) values (:id, :email, :status, :placedAt)")
                .param("id", order.orderId())
                .param("email", order.customerEmail())
                .param("status", order.status())
                .param("placedAt", order.placedAt().toString())
                .update();
        int lineNo = 0;
        for (LineItem item : order.items()) {
            jdbc.sql("insert into order_line_item (order_id, line_no, product_id, quantity) values (:id, :lineNo, :productId, :quantity)")
                    .param("id", order.orderId())
                    .param("lineNo", lineNo++)
                    .param("productId", item.productId().toString())
                    .param("quantity", item.quantity())
                    .update();
        }
    }

    Optional<StoredOrder> find(String orderId) {
        Optional<Header> header = jdbc.sql("select customer_email, status, placed_at from orders where order_id = :id")
                .param("id", orderId)
                .query((rs, n) -> new Header(rs.getString(1), rs.getString(2), rs.getString(3)))
                .optional();
        if (header.isEmpty()) {
            return Optional.empty();
        }
        List<LineItem> items = jdbc.sql("select product_id, quantity from order_line_item where order_id = :id order by line_no")
                .param("id", orderId)
                .query((rs, n) -> new LineItem(UUID.fromString(rs.getString(1)), rs.getInt(2)))
                .list();
        Header h = header.get();
        return Optional.of(new StoredOrder(orderId, h.email(), h.status(), Instant.parse(h.placedAt()), items));
    }

    boolean transition(String orderId, String from, String to) {
        return jdbc.sql("update orders set status = :to where order_id = :id and status = :from")
                .param("to", to)
                .param("id", orderId)
                .param("from", from)
                .update() == 1;
    }
}
