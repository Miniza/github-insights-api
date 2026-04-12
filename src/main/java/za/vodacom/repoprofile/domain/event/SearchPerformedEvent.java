package za.vodacom.repoprofile.domain.event;

/**
 * Domain event raised when a profile or repository search is performed.
 * Handled asynchronously to avoid blocking the request thread.
 */
public record SearchPerformedEvent(String username, String summary) {
}
