package foundation;

import foundation.audit.AuditLogger;
import foundation.messaging.MessagingConfiguration;
import foundation.outbox.OutboxConfiguration;
import foundation.problem.ProblemDetailsAdvice;
import foundation.security.SecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:foundation.properties")
@Import({
        ProblemDetailsAdvice.class,
        SecurityConfiguration.class,
        AuditLogger.class
        , MessagingConfiguration.class
        , OutboxConfiguration.class
})
public class FoundationConfiguration {
}
