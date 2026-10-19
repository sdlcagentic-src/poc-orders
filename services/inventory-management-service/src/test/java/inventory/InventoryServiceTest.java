package inventory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
class InventoryServiceTest {

    @Autowired
    InventoryService service;
    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void setUp() {
        jdbc.sql("delete from reservation_item").update();
        jdbc.sql("delete from reservation").update();
        jdbc.sql("delete from stock_level").update();
    }

    private UUID product(int quantity) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into stock_level (product_id, quantity) values (:p, :q)")
                .param("p", id).param("q", quantity).update();
        return id;
    }

    private int quantityOf(UUID id) {
        return jdbc.sql("select quantity from stock_level where product_id = :p").param("p", id)
                .query(Integer.class).single();
    }

    private int outboxCount() {
        return jdbc.sql("select count(*) from outbox_event").query(Integer.class).single();
    }

    private Api.ReserveStockRequest request(UUID order, UUID product, int quantity) {
        return new Api.ReserveStockRequest(order, List.of(new Api.ReservationItem(product, quantity)));
    }

    @Test
    void concurrentOrdersForTheLastUnitConfirmExactlyOne() throws Exception {
        UUID a = product(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<InventoryService.Outcome> task = () -> {
                start.await();
                return service.reserve(request(UUID.randomUUID(), a, 1));
            };
            Future<InventoryService.Outcome> first = pool.submit(task);
            Future<InventoryService.Outcome> second = pool.submit(task);
            start.countDown();

            InventoryService.Outcome one = first.get();
            InventoryService.Outcome two = second.get();

            long confirmed = List.of(one, two).stream().filter(o -> o.reservation() != null).count();
            long rejected = List.of(one, two).stream().filter(o -> o.reservation() == null).count();
            Assertions.assertEquals(1, confirmed);
            Assertions.assertEquals(1, rejected);
            Assertions.assertEquals(0, quantityOf(a));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void stockNeverGoesBelowZeroWhenRequestingMoreThanAvailable() {
        UUID a = product(2);

        InventoryService.Outcome outcome = service.reserve(request(UUID.randomUUID(), a, 3));

        Assertions.assertNull(outcome.reservation());
        Assertions.assertEquals(List.of(a), outcome.unavailable());
        Assertions.assertEquals(2, quantityOf(a));
    }

    @Test
    void repeatedRequestForTheSameOrderReservesOnce() {
        UUID a = product(5);
        UUID order = UUID.randomUUID();

        InventoryService.Outcome first = service.reserve(request(order, a, 2));
        InventoryService.Outcome second = service.reserve(request(order, a, 2));

        Assertions.assertEquals(first.reservation().reservationId(), second.reservation().reservationId());
        Assertions.assertEquals(3, quantityOf(a));
    }

    @Test
    void releaseReturnsReservedUnitsAndRecordsAnEvent() {
        UUID a = product(5);
        UUID order = UUID.randomUUID();
        service.reserve(request(order, a, 2));
        int before = outboxCount();

        boolean released = service.release(order);

        Assertions.assertTrue(released);
        Assertions.assertEquals(5, quantityOf(a));
        Assertions.assertEquals(before + 1, outboxCount());
        Assertions.assertFalse(service.release(order));
    }
}
