package za.vodacom.repoprofile.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import za.vodacom.repoprofile.application.dto.ErrorResponse;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("NotFoundException → 404")
    void testHandleNotFound() {
        String errorMessage = "GitHub user not found: octocat";
        NotFoundException exception = new NotFoundException(errorMessage);

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception);

        assertThat(response)
                .isNotNull()
                .extracting(ResponseEntity::getStatusCode)
                .asString()
                .contains("404");

        assertThat(response.getBody())
                .isNotNull()
                .extracting(ErrorResponse::status, ErrorResponse::error, ErrorResponse::message)
                .containsExactly(404, "Not Found", errorMessage);
    }

    @Test
    @DisplayName("ProviderApiException → 502")
    void testHandleProviderApiException() {
        String errorMessage = "GitHub API error: Connection timeout";
        ProviderApiException exception = new ProviderApiException(errorMessage);

        ResponseEntity<ErrorResponse> response = handler.handleProviderApi(exception);

        assertThat(response)
                .isNotNull()
                .extracting(ResponseEntity::getStatusCode)
                .asString()
                .contains("502");

        assertThat(response.getBody())
                .isNotNull()
                .extracting(ErrorResponse::status, ErrorResponse::error, ErrorResponse::message)
                .containsExactly(502, "Bad Gateway", errorMessage);
    }

    @Test
    @DisplayName("ProviderApiException with cause still returns 502")
    void testHandleProviderApiExceptionWithCause() {
        RuntimeException cause = new RuntimeException("Connection refused");
        ProviderApiException exception = new ProviderApiException("GitHub API error", cause);

        ResponseEntity<ErrorResponse> response = handler.handleProviderApi(exception);

        assertThat(response.getBody())
                .isNotNull()
                .extracting(ErrorResponse::status)
                .isEqualTo(502);
    }

    @Test
    @DisplayName("CircuitBreaker 'github' → 503")
    void testHandleCircuitBreakerGitHub() {
        CallNotPermittedException exception = mock(CallNotPermittedException.class);
        when(exception.getCausingCircuitBreakerName()).thenReturn("github");
        when(exception.getMessage()).thenReturn("CircuitBreaker 'github' is OPEN");

        ResponseEntity<ErrorResponse> response = handler.handleCircuitBreaker(exception);
        assertThat(response)
                .isNotNull()
                .extracting(ResponseEntity::getStatusCode)
                .asString()
                .contains("503");

        assertThat(response.getBody())
                .isNotNull()
                .extracting(ErrorResponse::status, ErrorResponse::error)
                .containsExactly(503, "Service Unavailable");

        assertThat(response.getBody().message())
                .contains("GitHub service is temporarily unavailable");
    }

    @Test
    @DisplayName("CircuitBreaker 'database' → 503")
    void testHandleCircuitBreakerDatabase() {
        CallNotPermittedException exception = mock(CallNotPermittedException.class);
        when(exception.getCausingCircuitBreakerName()).thenReturn("database");
        when(exception.getMessage()).thenReturn("CircuitBreaker 'database' is OPEN");

        ResponseEntity<ErrorResponse> response = handler.handleCircuitBreaker(exception);
        assertThat(response)
                .isNotNull()
                .extracting(ResponseEntity::getStatusCode)
                .asString()
                .contains("503");

        assertThat(response.getBody())
                .isNotNull()
                .extracting(ErrorResponse::status, ErrorResponse::error)
                .containsExactly(503, "Service Unavailable");

        assertThat(response.getBody().message())
                .contains("Database service is temporarily unavailable");
    }

    @Test
    @DisplayName("RateLimit → 429")
    void testHandleRateLimit() {
        RequestNotPermitted exception = mock(RequestNotPermitted.class);
        when(exception.getMessage()).thenReturn("Rate limit exceeded");

        ResponseEntity<ErrorResponse> response = handler.handleRateLimit(exception);
        assertThat(response)
                .isNotNull()
                .extracting(ResponseEntity::getStatusCode)
                .asString()
                .contains("429");

        assertThat(response.getBody())
                .isNotNull()
                .extracting(ErrorResponse::status, ErrorResponse::error)
                .containsExactly(429, "Too Many Requests");

        assertThat(response.getBody().message())
                .contains("Rate limit exceeded");
    }

    @Test
    @DisplayName("Error response includes timestamp")
    void testErrorResponseIncludesTimestamp() {
        NotFoundException exception = new NotFoundException("Not found");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception);

        assertThat(response.getBody())
                .isNotNull()
                .extracting(ErrorResponse::timestamp)
                .isNotNull();
    }

    @Test
    @DisplayName("Each exception type maps to distinct status")
    void testHandleMultipleDifferentExceptions() {
        NotFoundException notFoundEx = new NotFoundException("Not found");
        ProviderApiException apiEx = new ProviderApiException("API error");
        CallNotPermittedException cbEx = mock(CallNotPermittedException.class);
        when(cbEx.getCausingCircuitBreakerName()).thenReturn("github");
        when(cbEx.getMessage()).thenReturn("CB open");

        ResponseEntity<ErrorResponse> response1 = handler.handleNotFound(notFoundEx);
        ResponseEntity<ErrorResponse> response2 = handler.handleProviderApi(apiEx);
        ResponseEntity<ErrorResponse> response3 = handler.handleCircuitBreaker(cbEx);

        assertThat(response1.getBody().status()).isEqualTo(404);
        assertThat(response2.getBody().status()).isEqualTo(502);
        assertThat(response3.getBody().status()).isEqualTo(503);
    }
}
