package za.vodacom.repoprofile.exception;

/**
 * Thrown when a source-code hosting provider (GitHub, GitLab, Bitbucket) returns
 * an unexpected error or is unreachable.
 */
public class ProviderApiException extends RuntimeException {

    public ProviderApiException(String message) {
        super(message);
    }

    public ProviderApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
