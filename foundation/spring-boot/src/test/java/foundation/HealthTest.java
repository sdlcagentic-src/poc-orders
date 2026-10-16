package foundation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthTest {

    @Value("${local.server.port}")
    int port;

    @Test
    void theHealthProbesAreOpenWithoutCredentials() throws Exception {
        assertThat(Http.get(port, "/actuator/health").statusCode()).isEqualTo(200);
        assertThat(Http.get(port, "/actuator/health").body()).contains("\"status\":\"UP\"");
        assertThat(Http.get(port, "/actuator/health/liveness").statusCode()).isEqualTo(200);
        assertThat(Http.get(port, "/actuator/health/readiness").statusCode()).isEqualTo(200);
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        assertThat(Http.get(port, "/actuator/env", "Authorization", "Bearer test-token").statusCode()).isIn(401, 403, 404);
        assertThat(Http.get(port, "/actuator/env").body()).doesNotContain("propertySources");
    }
}
