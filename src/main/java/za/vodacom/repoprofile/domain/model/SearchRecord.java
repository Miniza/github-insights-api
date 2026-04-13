package za.vodacom.repoprofile.domain.model;

import java.time.Instant;

/**
 * Domain representation of a persisted search history entry.
 * Decouples the port layer from JPA entity classes.
 */
public record SearchRecord(
        Long id,
        String username,
        String summary,
        Instant searchedAt
) {
}
