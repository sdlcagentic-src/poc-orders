package foundation;

import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableFoundation
public class TestApplication {

    /** Accepts the token "good" and rejects everything else, so no identity provider is needed. A real service configures its issuer or key set. */
    @Bean
    JwtDecoder jwtDecoder() {
        return token -> {
            if (!token.equals("good")) {
                throw new BadJwtException("rejected");
            }
            return Jwt.withTokenValue(token).header("alg", "none").subject("alice").build();
        };
    }
}
