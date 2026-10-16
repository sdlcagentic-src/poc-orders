package foundation.problem;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every error of a service is an RFC 7807 problem document. Spring's own exceptions keep their status and title; anything unexpected
 * becomes a 500 with a fixed text, so a message, a class name or a stack trace never reaches a client. The trace id is added so a
 * client can quote it. Details stay in the log.
 */
@RestControllerAdvice
public class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ProblemDetailsAdvice.class);

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception e, WebRequest request) {
        LOG.error("unexpected error", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setTitle("Internal Server Error");
        return handleExceptionInternal(e, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             org.springframework.http.HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            if (problem.getType() == null || problem.getType().equals(URI.create("about:blank"))) {
                problem.setType(URI.create("about:blank"));
            }
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                problem.setProperty("traceId", traceId);
            }
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}
