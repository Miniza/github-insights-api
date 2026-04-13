package za.vodacom.repoprofile.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Custom Exception Unit Tests")
class CustomExceptionTest {

    @Test
    @DisplayName("Should create NotFoundException with message")
    void testNotFoundExceptionCreation() {
        // Arrange
        String message = "User not found";

        // Act
        NotFoundException exception = new NotFoundException(message);

        // Assert
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage(message);
    }

    @Test
    @DisplayName("Should create ProviderApiException with message only")
    void testProviderApiExceptionWithMessageOnly() {
        // Arrange
        String message = "GitHub API error";

        // Act
        ProviderApiException exception = new ProviderApiException(message);

        // Assert
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage(message)
                .hasNoCause();
    }

    @Test
    @DisplayName("Should create ProviderApiException with message and cause")
    void testProviderApiExceptionWithCause() {
        // Arrange
        String message = "GitHub API error";
        RuntimeException cause = new RuntimeException("Connection timeout");

        // Act
        ProviderApiException exception = new ProviderApiException(message, cause);

        // Assert
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage(message)
                .hasCause(cause);
    }

    @Test
    @DisplayName("Should throw NotFoundException")
    void testThrowNotFoundException() {
        // Act & Assert
        assertThatThrownBy(() -> {
            throw new NotFoundException("Resource not found");
        })
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Resource not found");
    }

    @Test
    @DisplayName("Should throw ProviderApiException")
    void testThrowProviderApiException() {
        // Act & Assert
        assertThatThrownBy(() -> {
            throw new ProviderApiException("API unreachable");
        })
                .isInstanceOf(ProviderApiException.class)
                .hasMessage("API unreachable");
    }
}
