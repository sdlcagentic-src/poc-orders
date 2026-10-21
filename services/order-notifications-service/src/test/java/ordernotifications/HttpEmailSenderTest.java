package ordernotifications;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class HttpEmailSenderTest {

    private static final EmailMessage MESSAGE = new EmailMessage("customer@example.com", "subject", "body");

    @Test
    void messageIsPostedToTheGatewayWithBearerToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://gateway/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess());

        new HttpEmailSender(builder, "http://gateway", "test-token").send(MESSAGE);

        server.verify();
    }

    @Test
    void gatewayFailureSurfacesAsAnError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://gateway/emails")).andRespond(withServerError());

        HttpEmailSender sender = new HttpEmailSender(builder, "http://gateway", "");

        assertThrows(RestClientException.class, () -> sender.send(MESSAGE));
    }
}
