package ordernotifications;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Sends emails through an HTTP email gateway. */
public class HttpEmailSender implements EmailSender {

    private final RestClient client;
    private final String token;

    public HttpEmailSender(RestClient.Builder builder, String gatewayUrl, String token) {
        this.client = builder.baseUrl(gatewayUrl).build();
        this.token = token;
    }

    @Override
    public void send(EmailMessage message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("to", message.to());
        body.put("subject", message.subject());
        body.put("body", message.body());

        RestClient.RequestBodySpec request = client.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON);
        if (token != null && !token.isBlank()) {
            request = request.headers(headers -> headers.setBearerAuth(token));
        }
        request.body(body).retrieve().toBodilessEntity();
    }
}
