package foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BearerRequiredTest {

    @Value("${local.server.port}")
    int port;

    @Test
    void aRequestWithoutATokenIsAnUnauthorizedProblem() throws Exception {
        HttpResponse<String> response = Http.get(port, "/ok");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(response.body()).contains("\"status\":401");
    }

    @Test
    void aRejectedTokenIsUnauthorizedAndAGoodTokenIsLetThrough() throws Exception {
        assertThat(Http.get(port, "/ok", "Authorization", "Bearer bad").statusCode()).isEqualTo(401);
        assertThat(Http.get(port, "/ok", "Authorization", "Bearer good").statusCode()).isEqualTo(200);
    }
}
