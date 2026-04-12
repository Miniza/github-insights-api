package za.vodacom.repoprofile.application.dto;

import java.time.Instant;

public record SearchSummary(
        Long id,
        String username,
        String summary,
        Instant searchedAt
) {
}
