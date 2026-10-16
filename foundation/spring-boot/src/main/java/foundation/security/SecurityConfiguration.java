package foundation.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless API security. The health probes are always open. With {@code auth=jwt} (jwt here) every other request needs a valid bearer token
 * and a missing or rejected token is answered with an RFC 7807 problem; with {@code auth=none} every request is allowed.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain foundationSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> {
                    requests.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                    requests.anyRequest().authenticated();
                });
        http.oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults())
                .authenticationEntryPoint((request, response, e) -> problem(response, 401, "Unauthorized", "Authentication is required."))
                .accessDeniedHandler((request, response, e) -> problem(response, 403, "Forbidden", "Access is denied.")));
        return http.build();
    }

    private static void problem(jakarta.servlet.http.HttpServletResponse response, int status, String title, String detail) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}
