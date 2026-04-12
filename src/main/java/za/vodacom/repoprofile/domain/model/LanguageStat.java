package za.vodacom.repoprofile.domain.model;

public record LanguageStat(
        String language,
        long count,
        long totalSize
) {
}
