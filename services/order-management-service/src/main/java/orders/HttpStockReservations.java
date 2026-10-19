package orders;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class HttpStockReservations implements StockReservations {

    private final RestClient client;

    @Autowired
    public HttpStockReservations(@Value("${inventory.base-url}") String baseUrl) {
        this(RestClient.builder().baseUrl(baseUrl).build());
    }

    HttpStockReservations(RestClient client) {
        this.client = client;
    }

    @Override
    public StockReservationResult reserve(UUID orderId, List<LineItem> items, String authorization) {
        String body = "{\"orderId\":" + Json.quote(orderId.toString()) + ",\"items\":" + Json.itemsArray(items) + "}";
        try {
            client.post()
                    .uri("/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (authorization != null) {
                            headers.set(HttpHeaders.AUTHORIZATION, authorization);
                        }
                    })
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return StockReservationResult.RESERVED;
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            if (status == 409) {
                return StockReservationResult.OUT_OF_STOCK;
            }
            if (status == 422) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "A product is unknown or a quantity is invalid.");
            }
            throw unavailable();
        } catch (RestClientException e) {
            throw unavailable();
        }
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stock could not be reserved.");
    }
}
