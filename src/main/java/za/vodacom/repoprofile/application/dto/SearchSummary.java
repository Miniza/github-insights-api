package za.vodacom.repoprofile.application.dto;

import java.time.ZonedDateTime;

public record SearchSummary(
        Long id,
        String username,
        String summary,
        ZonedDateTime searchedAt
) {
}
