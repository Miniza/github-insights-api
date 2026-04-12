package za.vodacom.repoprofile.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            MDC.put("correlationId", correlationId);
            MDC.put("method", request.getMethod());
            MDC.put("uri", request.getRequestURI());
            MDC.put("queryString", request.getQueryString() != null ? request.getQueryString() : "");
            MDC.put("clientIp", request.getRemoteAddr());

            log.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());

            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = wrappedResponse.getStatus();

            MDC.put("durationMs", String.valueOf(duration));
            MDC.put("httpStatus", String.valueOf(status));

            String requestBody = extractBody(wrappedRequest.getContentAsByteArray());
            String responseBody = extractBody(wrappedResponse.getContentAsByteArray());

            MDC.put("requestBody", requestBody);
            MDC.put("responseBody", responseBody);

            log.info("Completed request: {} {} | status={} | duration={}ms",
                    request.getMethod(), request.getRequestURI(), status, duration);

            // Copy response body back to the actual response
            wrappedResponse.copyBodyToResponse();

            MDC.clear();
        }
    }

    private String extractBody(byte[] content) {
        if (content == null || content.length == 0) {
            return "";
        }
        int maxLength = 2048;
        String body = new String(content, StandardCharsets.UTF_8);
        return body.length() > maxLength ? body.substring(0, maxLength) + "...[truncated]" : body;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.startsWith("/h2-console");
    }
}
