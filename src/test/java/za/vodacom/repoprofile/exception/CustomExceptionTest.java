package za.vodacom.repoprofile.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Custom Exception Unit Tests")
class CustomExceptionTest {

    @Test
    @DisplayName("NotFoundException with message")
    void testNotFoundExceptionCreation() {
        String message = "User not found";

        NotFoundException exception = new NotFoundException(message);
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage(message);
    }

    @Test
    @DisplayName("ProviderApiException - message only")
    void testProviderApiExceptionWithMessageOnly() {
        String message = "GitHub API error";

        ProviderApiException exception = new ProviderApiException(message);
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage(message)
                .hasNoCause();
    }

    @Test
    @DisplayName("ProviderApiException with cause")
    void testProviderApiExceptionWithCause() {
        String message = "GitHub API error";
        RuntimeException cause = new RuntimeException("Connection timeout");

        ProviderApiException exception = new ProviderApiException(message, cause);
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage(message)
                .hasCause(cause);
    }

    @Test
    @DisplayName("Throw + catch NotFoundException")
    void testThrowNotFoundException() {
        assertThatThrownBy(() -> {
            throw new NotFoundException("Resource not found");
        })
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Resource not found");
    }

    @Test
    @DisplayName("Throw + catch ProviderApiException")
    void testThrowProviderApiException() {
        assertThatThrownBy(() -> {
            throw new ProviderApiException("API unreachable");
        })
                .isInstanceOf(ProviderApiException.class)
                .hasMessage("API unreachable");
    }
}
