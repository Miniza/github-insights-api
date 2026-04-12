package za.vodacom.repoprofile.domain.model;

public record Repo(
        String name,
        String description,
        String htmlUrl,
        String language,
        int stargazersCount,
        int forksCount,
        long size
) {
}
