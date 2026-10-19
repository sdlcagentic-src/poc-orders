package orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class HttpStockReservationsTest {

    private MockRestServiceServer server;
    private HttpStockReservations client;
    private final List<LineItem> items = List.of(new LineItem(UUID.randomUUID(), 3));

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://inventory");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpStockReservations(builder.build());
    }

    @Test
    void reservationIsPostedWithTheBearerTokenOfTheRequest() {
        server.expect(requestTo("http://inventory/reservations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));

        assertEquals(StockReservationResult.RESERVED, client.reserve(UUID.randomUUID(), items, "Bearer abc"));
        server.verify();
    }

    @Test
    void conflictMeansOutOfStock() {
        server.expect(requestTo("http://inventory/reservations")).andRespond(withStatus(HttpStatus.CONFLICT));

        assertEquals(StockReservationResult.OUT_OF_STOCK, client.reserve(UUID.randomUUID(), items, "Bearer abc"));
    }

    @Test
    void unprocessableAnswerIsPassedOnAs422() {
        server.expect(requestTo("http://inventory/reservations")).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> client.reserve(UUID.randomUUID(), items, "Bearer abc"));
        assertEquals(422, e.getStatusCode().value());
    }

    @Test
    void unauthorisedAnswerSurfacesAsBadGateway() {
        server.expect(requestTo("http://inventory/reservations")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> client.reserve(UUID.randomUUID(), items, null));
        assertEquals(502, e.getStatusCode().value());
    }

    @Test
    void serverErrorSurfacesAsBadGateway() {
        server.expect(requestTo("http://inventory/reservations")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> client.reserve(UUID.randomUUID(), items, "Bearer abc"));
        assertEquals(502, e.getStatusCode().value());
    }
}
