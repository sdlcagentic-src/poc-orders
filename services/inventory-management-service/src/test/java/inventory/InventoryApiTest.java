package inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class InventoryApiTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    JdbcClient jdbc;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
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

    private String body(UUID order, UUID p1, int q1, UUID p2, int q2) {
        return String.format(
                "{\"orderId\":\"%s\",\"items\":[{\"productId\":\"%s\",\"quantity\":%d},{\"productId\":\"%s\",\"quantity\":%d}]}",
                order, p1, q1, p2, q2);
    }

    @Test
    void orderIsRejectedWhenOneProductIsOutOfStock() throws Exception {
        UUID a = product(5);
        UUID b = product(0);

        mvc.perform(post("/reservations").with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID(), a, 3, b, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Out of stock: " + b));

        Assertions.assertEquals(5, quantityOf(a));
        Assertions.assertEquals(0, quantityOf(b));
    }

    @Test
    void orderIsAcceptedAndBothItemsAreReserved() throws Exception {
        UUID a = product(5);
        UUID b = product(3);

        mvc.perform(post("/reservations").with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID(), a, 2, b, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").exists())
                .andExpect(jsonPath("$.reservedAt").exists())
                .andExpect(jsonPath("$.items.length()").value(2));

        Assertions.assertEquals(3, quantityOf(a));
        Assertions.assertEquals(1, quantityOf(b));
    }

    @Test
    void stockLevelsShowQuantityPerProduct() throws Exception {
        UUID a = product(5);
        UUID b = product(12);

        mvc.perform(get("/stock-levels").with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[?(@.productId=='" + a + "')].quantity").value(5))
                .andExpect(jsonPath("$.items[?(@.productId=='" + b + "')].quantity").value(12));
    }

    @Test
    void unknownProductIsUnprocessable() throws Exception {
        UUID a = product(5);

        mvc.perform(post("/reservations").with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID(), a, 1, UUID.randomUUID(), 1)))
                .andExpect(status().isUnprocessableEntity());

        Assertions.assertEquals(5, quantityOf(a));
    }

    @Test
    void nonPositiveQuantityIsUnprocessable() throws Exception {
        UUID a = product(5);
        UUID b = product(5);

        mvc.perform(post("/reservations").with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID(), a, 0, b, 1)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void requestWithoutItemsIsBadRequest() throws Exception {
        mvc.perform(post("/reservations").with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON).content("{\"orderId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stockCheckedEventIsRecordedForAcceptedAndRejectedOrders() throws Exception {
        UUID a = product(1);
        UUID b = product(1);
        int before = outboxCount();

        mvc.perform(post("/reservations").with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID(), a, 1, b, 1)))
                .andExpect(status().isCreated());
        Assertions.assertEquals(before + 1, outboxCount());

        mvc.perform(post("/reservations").with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID(), a, 1, b, 1)))
                .andExpect(status().isConflict());
        Assertions.assertEquals(before + 2, outboxCount());
    }
}
