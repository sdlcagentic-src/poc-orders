package foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProblemDetailsTest {

    @Value("${local.server.port}")
    int port;

    private HttpResponse<String> get(String path) throws Exception {
        return Http.get(port, path, "Authorization", "Bearer good");
    }

    @Test
    void aMissingParameterIsAProblemDocument() throws Exception {
        HttpResponse<String> response = get("/needs-param");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(response.body()).contains("\"status\":400").contains("\"title\"");
    }

    @Test
    void anUnknownPathIsAProblemDocument() throws Exception {
        HttpResponse<String> response = get("/no-such-path");
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
    }

    @Test
    void anUnexpectedErrorIsA500WithNoInternalDetail() throws Exception {
        HttpResponse<String> response = get("/boom");
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(response.body()).contains("Internal Server Error").doesNotContain("secret internal detail").doesNotContain("IllegalStateException");
    }
}
