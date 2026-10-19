package orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Import(TestPorts.class)
class OrderApiTest {

    @Autowired WebApplicationContext context;
    @Autowired FakeStockReservations fake;
    @Autowired JdbcClient jdbc;
    @Autowired OrderRepository repository;
    @Autowired StockCheckedListener listener;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        fake.reset();
    }

    private static String j(String s) {
        return s.replace('\'', '"');
    }

    private static String idOf(String json) {
        Matcher m = Pattern.compile("\"orderId\":\"([^\"]+)\"").matcher(json);
        assertTrue(m.find());
        return m.group(1);
    }

    private String twoItemBody() {
        return j(("{'customerEmail':'buyer@example.com','lineItems':["
                + "{'productId':'%s','quantity':3},{'productId':'%s','quantity':2}]}")
                .formatted(UUID.randomUUID(), UUID.randomUUID()));
    }

    private MockHttpServletResponse place(String json) throws Exception {
        return mvc.perform(post("/orders").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResponse();
    }

    private MockHttpServletResponse view(String id) throws Exception {
        return mvc.perform(get("/orders/" + id).with(jwt())).andReturn().getResponse();
    }

    private MockHttpServletResponse cancel(String id) throws Exception {
        return mvc.perform(post("/orders/" + id + "/cancellation").with(jwt())).andReturn().getResponse();
    }

    private String placeConfirmed() throws Exception {
        MockHttpServletResponse response = place(twoItemBody());
        assertEquals(201, response.getStatus());
        return idOf(response.getContentAsString());
    }

    private String seedPlaced() {
        String id = UUID.randomUUID().toString();
        repository.insert(new StoredOrder(id, "seed@example.com", "placed",
                Instant.now().truncatedTo(ChronoUnit.MILLIS), List.of(new LineItem(UUID.randomUUID(), 1))));
        return id;
    }

    private String statusInDb(String id) {
        return jdbc.sql("select status from orders where order_id = :id").param("id", id).query(String.class).single();
    }

    private long orderCount() {
        return jdbc.sql("select count(*) from orders").query(Long.class).single();
    }

    private long outboxCount(String topic) {
        return jdbc.sql("select * from outbox_event").query().listOfRows().stream()
                .filter(row -> row.values().stream().anyMatch(v -> v != null && v.toString().equals(topic)))
                .count();
    }

    private ConsumerRecord<String, String> stockChecked(long offset, String payload) {
        return new ConsumerRecord<>("inventory.stock.checked", 0, offset, null, payload);
    }

    private static long uniqueOffset() {
        return System.nanoTime();
    }

    @Test
    void placingAnOrderWithTwoProductsRecordsBothLineItemsAndQuantities() throws Exception {
        MockHttpServletResponse response = place(twoItemBody());

        assertEquals(201, response.getStatus());
        String body = response.getContentAsString();
        String id = idOf(body);
        assertTrue(body.contains("\"quantity\":3") && body.contains("\"quantity\":2"));
        List<Integer> quantities = jdbc.sql("select quantity from order_line_item where order_id = :id order by line_no")
                .param("id", id).query(Integer.class).list();
        assertEquals(List.of(3, 2), quantities);
        assertEquals(2, fake.lastItems.size());
    }

    @Test
    void placingAnOrderReservesStockAndPublishesEvents() throws Exception {
        long placedBefore = outboxCount("orders.placed");
        long confirmedBefore = outboxCount("orders.confirmed");

        MockHttpServletResponse response = place(twoItemBody());

        assertEquals(201, response.getStatus());
        assertEquals(1, fake.reservedOrders.size());
        assertTrue(response.getContentAsString().contains("\"status\":\"confirmed\""));
        assertEquals(placedBefore + 1, outboxCount("orders.placed"));
        assertEquals(confirmedBefore + 1, outboxCount("orders.confirmed"));
    }

    @Test
    void anOrderWhoseItemsAreOutOfStockIsRejected() throws Exception {
        fake.result = StockReservationResult.OUT_OF_STOCK;
        long rejectedBefore = outboxCount("orders.rejected");

        MockHttpServletResponse response = place(twoItemBody());

        assertEquals(201, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"status\":\"rejected\""));
        assertEquals(rejectedBefore + 1, outboxCount("orders.rejected"));
    }

    @Test
    void anOrderWithoutLineItemsIsUnprocessable() throws Exception {
        MockHttpServletResponse response = place(j("{'customerEmail':'buyer@example.com','lineItems':[]}"));

        assertEquals(422, response.getStatus());
    }

    @Test
    void anInvalidQuantityIsUnprocessable() throws Exception {
        MockHttpServletResponse response = place(j(
                "{'customerEmail':'buyer@example.com','lineItems':[{'productId':'" + UUID.randomUUID() + "','quantity':0}]}"));

        assertEquals(422, response.getStatus());
    }

    @Test
    void aMissingEmailIsABadRequest() throws Exception {
        MockHttpServletResponse response = place(j(
                "{'lineItems':[{'productId':'" + UUID.randomUUID() + "','quantity':1}]}"));

        assertEquals(400, response.getStatus());
    }

    @Test
    void aMalformedBodyIsABadRequest() throws Exception {
        MockHttpServletResponse response = place("{not json");

        assertEquals(400, response.getStatus());
    }

    @Test
    void whenInventoryCannotBeReachedNoOrderIsStored() throws Exception {
        fake.failure = new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        long before = orderCount();

        MockHttpServletResponse response = place(twoItemBody());

        assertEquals(502, response.getStatus());
        assertEquals(before, orderCount());
    }

    @Test
    void aPlacedOrderShowsItsCurrentStatus() throws Exception {
        String id = seedPlaced();

        MockHttpServletResponse response = view(id);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"status\":\"placed\""));
    }

    @Test
    void aConfirmedOrderShowsItsStatusAndLineItems() throws Exception {
        String id = placeConfirmed();

        MockHttpServletResponse response = view(id);

        assertEquals(200, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains("\"status\":\"confirmed\""));
        assertTrue(body.contains("\"placedAt\""));
    }

    @Test
    void aRejectedOrderShowsItsStatus() throws Exception {
        fake.result = StockReservationResult.OUT_OF_STOCK;
        String id = idOf(place(twoItemBody()).getContentAsString());

        assertTrue(view(id).getContentAsString().contains("\"status\":\"rejected\""));
    }

    @Test
    void anUnknownOrderIsNotFound() throws Exception {
        assertEquals(404, view(UUID.randomUUID().toString()).getStatus());
        assertEquals(404, view("not-an-id").getStatus());
    }

    @Test
    void cancellingAConfirmedOrderMakesItCancelledAndAnnouncesIt() throws Exception {
        String id = placeConfirmed();
        long before = outboxCount("orders.cancelled");

        MockHttpServletResponse response = cancel(id);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"status\":\"cancelled\""));
        assertEquals("cancelled", statusInDb(id));
        assertEquals(before + 1, outboxCount("orders.cancelled"));
    }

    @Test
    void anOrderCannotBeCancelledTwice() throws Exception {
        String id = placeConfirmed();
        assertEquals(200, cancel(id).getStatus());

        assertEquals(409, cancel(id).getStatus());
    }

    @Test
    void aRejectedOrderCannotBeCancelled() throws Exception {
        fake.result = StockReservationResult.OUT_OF_STOCK;
        String id = idOf(place(twoItemBody()).getContentAsString());

        assertEquals(409, cancel(id).getStatus());
    }

    @Test
    void cancellingAnUnknownOrderIsNotFound() throws Exception {
        assertEquals(404, cancel(UUID.randomUUID().toString()).getStatus());
    }

    @Test
    void aConfirmedOrderIsStoredAndStillRetrievableFromTheDatabase() throws Exception {
        String id = placeConfirmed();

        assertEquals("confirmed", statusInDb(id));
        assertEquals(200, view(id).getStatus());
        assertTrue(repository.find(id).isPresent());
    }

    @Test
    void atLeast95Of100PlacementsCompleteWithinTwoSeconds() throws Exception {
        int fast = 0;
        for (int i = 0; i < 100; i++) {
            String body = twoItemBody();
            long start = System.nanoTime();
            MockHttpServletResponse response = place(body);
            long millis = (System.nanoTime() - start) / 1_000_000;
            if (response.getStatus() == 201 && millis <= 2000) {
                fast++;
            }
        }

        assertTrue(fast >= 95, "only " + fast + " requests were fast enough");
    }

    @Test
    void anAvailableStockCheckConfirmsAPlacedOrder() {
        String id = seedPlaced();
        long before = outboxCount("orders.confirmed");

        listener.onStockChecked(stockChecked(uniqueOffset(),
                j("{'orderId':'" + id + "','allAvailable':true,'unavailableProductIds':[]}")));

        assertEquals("confirmed", statusInDb(id));
        assertEquals(before + 1, outboxCount("orders.confirmed"));
    }

    @Test
    void anUnavailableStockCheckRejectsAPlacedOrder() {
        String id = seedPlaced();
        long before = outboxCount("orders.rejected");

        listener.onStockChecked(stockChecked(uniqueOffset(), j("{'orderId':'" + id
                + "','allAvailable':false,'unavailableProductIds':['" + UUID.randomUUID() + "']}")));

        assertEquals("rejected", statusInDb(id));
        assertEquals(before + 1, outboxCount("orders.rejected"));
    }

    @Test
    void aRedeliveredStockCheckMessageIsAppliedOnlyOnce() {
        long offset = uniqueOffset();
        String first = seedPlaced();
        String second = seedPlaced();
        String payloadTemplate = "{'orderId':'%s','allAvailable':true,'unavailableProductIds':[]}";

        listener.onStockChecked(stockChecked(offset, j(payloadTemplate.formatted(first))));
        listener.onStockChecked(stockChecked(offset, j(payloadTemplate.formatted(second))));

        assertEquals("confirmed", statusInDb(first));
        assertEquals("placed", statusInDb(second));
    }

    @Test
    void aStockCheckForAnUnknownOrderIsIgnored() {
        listener.onStockChecked(stockChecked(uniqueOffset(), j("{'orderId':'" + UUID.randomUUID()
                + "','allAvailable':true,'unavailableProductIds':[]}")));
    }
}
