package orders;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class TestPorts {

    @Bean
    @Primary
    FakeStockReservations fakeStockReservations() {
        return new FakeStockReservations();
    }
}
