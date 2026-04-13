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
    @DisplayName("Should handle NotFoundException with 404 status")
    void testHandleNotFound() {
        // Arrange
        String errorMessage = "GitHub user not found: octocat";
        NotFoundException exception = new NotFoundException(errorMessage);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception);

        // Assert
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
    @DisplayName("Should handle ProviderApiException with 502 status")
    void testHandleProviderApiException() {
        // Arrange
        String errorMessage = "GitHub API error: Connection timeout";
        ProviderApiException exception = new ProviderApiException(errorMessage);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleProviderApi(exception);

        // Assert
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
    @DisplayName("Should handle ProviderApiException with cause")
    void testHandleProviderApiExceptionWithCause() {
        // Arrange
        RuntimeException cause = new RuntimeException("Connection refused");
        ProviderApiException exception = new ProviderApiException("GitHub API error", cause);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleProviderApi(exception);

        // Assert
        assertThat(response.getBody())
                .isNotNull()
                .extracting(ErrorResponse::status)
                .isEqualTo(502);
    }

    @Test
    @DisplayName("Should handle CircuitBreaker exception for GitHub")
    void testHandleCircuitBreakerGitHub() {
        // Arrange — CallNotPermittedException has no public constructor; use Mockito
        CallNotPermittedException exception = mock(CallNotPermittedException.class);
        when(exception.getCausingCircuitBreakerName()).thenReturn("github");
        when(exception.getMessage()).thenReturn("CircuitBreaker 'github' is OPEN");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleCircuitBreaker(exception);

        // Assert
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
    @DisplayName("Should handle CircuitBreaker exception for database")
    void testHandleCircuitBreakerDatabase() {
        // Arrange
        CallNotPermittedException exception = mock(CallNotPermittedException.class);
        when(exception.getCausingCircuitBreakerName()).thenReturn("database");
        when(exception.getMessage()).thenReturn("CircuitBreaker 'database' is OPEN");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleCircuitBreaker(exception);

        // Assert
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
    @DisplayName("Should handle RateLimit exception with 429 status")
    void testHandleRateLimit() {
        // Arrange — RequestNotPermitted has no public constructor; use Mockito
        RequestNotPermitted exception = mock(RequestNotPermitted.class);
        when(exception.getMessage()).thenReturn("Rate limit exceeded");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleRateLimit(exception);

        // Assert
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
    @DisplayName("Should include timestamp in error response")
    void testErrorResponseIncludesTimestamp() {
        // Arrange
        NotFoundException exception = new NotFoundException("Not found");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception);

        // Assert
        assertThat(response.getBody())
                .isNotNull()
                .extracting(ErrorResponse::timestamp)
                .isNotNull();
    }

    @Test
    @DisplayName("Should handle multiple different exceptions")
    void testHandleMultipleDifferentExceptions() {
        // Arrange
        NotFoundException notFoundEx = new NotFoundException("Not found");
        ProviderApiException apiEx = new ProviderApiException("API error");
        CallNotPermittedException cbEx = mock(CallNotPermittedException.class);
        when(cbEx.getCausingCircuitBreakerName()).thenReturn("github");
        when(cbEx.getMessage()).thenReturn("CB open");

        // Act
        ResponseEntity<ErrorResponse> response1 = handler.handleNotFound(notFoundEx);
        ResponseEntity<ErrorResponse> response2 = handler.handleProviderApi(apiEx);
        ResponseEntity<ErrorResponse> response3 = handler.handleCircuitBreaker(cbEx);

        // Assert
        assertThat(response1.getBody().status()).isEqualTo(404);
        assertThat(response2.getBody().status()).isEqualTo(502);
        assertThat(response3.getBody().status()).isEqualTo(503);
    }
}
