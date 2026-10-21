package ordernotifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class EmailConfiguration {

    @Bean
    EmailSender emailSender(@Value("${notifications.email.gateway-url}") String gatewayUrl,
                            @Value("${notifications.email.gateway-token:}") String token) {
        return new HttpEmailSender(RestClient.builder(), gatewayUrl, token);
    }
}
